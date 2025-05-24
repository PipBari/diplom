package ru.backend.service.git;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;
import ru.backend.rest.application.dto.ApplicationDto;
import ru.backend.rest.git.dto.FileNodeDto;
import ru.backend.rest.git.dto.GitConnectionRequestDto;
import ru.backend.rest.settings.dto.ServersDto;
import ru.backend.service.application.ApplicationService;
import ru.backend.service.settings.ServersService;
import ru.backend.util.EncryptionUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Service
@RequiredArgsConstructor
public class GitflowGeneratorService {

    private final GitWriterService gitWriterService;
    private final ApplicationService applicationService;
    private final GitService gitService;
    private final ServersService serversService;

    public void generate(ApplicationDto app, ServersDto server, GitConnectionRequestDto repo)
            throws IOException, GitAPIException {

        String appPath = app.getPath();
        String branch = app.getBranch();
        String remoteDir = "/home/" + server.getSpecify_username() + "/apps/" + app.getName();

        List<FileNodeDto> files = gitWriterService.listFiles(repo, branch, appPath);

        String deployScriptContent = generateDeploySh(app, server, files, repo);
        String workflowContent = generateDeployYml(app, server);
        String rollbackScriptContent = generateRollbackSh(app);

        gitWriterService.pushFile(
                repo,
                branch,
                appPath,
                "deploy.sh",
                deployScriptContent,
                "Добавлен автоматический deploy.sh"
        );

        gitWriterService.pushFile(
                repo,
                branch,
                ".github/workflows",
                "deploy.yml",
                workflowContent,
                "Добавлен GitHub Actions workflow"
        );

        gitWriterService.pushFile(
                repo,
                branch,
                appPath,
                "rollback.sh",
                rollbackScriptContent,
                "Добавлен автоматический rollback.sh"
        );

        try {
            Map<String, String> scripts = new HashMap<>();
            scripts.put("deploy.sh", deployScriptContent);
            scripts.put("rollback.sh", rollbackScriptContent);

            uploadFilesToServer(remoteDir, server, scripts);
            runDeployScriptOverSsh(server, remoteDir);
        } catch (Exception ex) {
            gitWriterService.revertLastCommit(repo, branch);
            throw new RuntimeException("Откат последнего коммита в Git из-за ошибки при деплое", ex);
        }
    }

    private void runDeployScriptOverSsh(ServersDto server, String remoteDir) {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(server.getSpecify_username(), server.getHost(), server.getPort());
            session.setPassword(EncryptionUtils.decrypt(server.getPassword()));

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect(10000);

            String command = "cd " + remoteDir + " && bash ./deploy.sh";

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);
            channel.setErrStream(System.err);
            channel.setOutputStream(System.out);
            channel.connect();

            while (!channel.isClosed()) {
                Thread.sleep(100);
            }

            int exitCode = channel.getExitStatus();
            channel.disconnect();
            session.disconnect();

            if (exitCode != 0) {
                throw new RuntimeException("deploy.sh завершился с ошибкой (exit code: " + exitCode + ")");
            }

        } catch (Exception e) {
            throw new RuntimeException("Не удалось выполнить deploy.sh по SSH: " + e.getMessage(), e);
        }
    }

    private String generateDeploySh(ApplicationDto app, ServersDto server, List<FileNodeDto> files, GitConnectionRequestDto repo) {
        String appDir = "/home/" + server.getSpecify_username() + "/apps/" + app.getName();
        String repoUrl = repo.getRepoUrl();
        String repoPath = repoUrl.replace("https://github.com/", "").replace(".git", "");

        boolean hasTerraform = files.stream().anyMatch(f -> f.getName().endsWith(".tf"));
        boolean hasAnsible = files.stream().anyMatch(f -> f.getName().endsWith(".yml") || f.getName().endsWith(".yaml"));
        boolean hasBash = files.stream().anyMatch(f -> f.getName().endsWith(".sh"));

        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/bash\n");
        sb.append("set -e\n\n");
        sb.append("echo \"Начинается деплой %s...\"\n".formatted(app.getName()));
        sb.append("cd %s\n\n".formatted(appDir));
        sb.append("echo \"GIT_USERNAME: $GIT_USERNAME\"\n");
        sb.append("echo \"GIT_TOKEN длина: ${#GIT_TOKEN} символов\"\n\n");
        sb.append("if [ ! -d .git ]; then\n");
        sb.append("  echo 'Репозиторий не найден, клонируем...'\n");
        sb.append("  rm -rf .tmp_clone\n");
        sb.append("  git clone https://${GIT_USERNAME}:${GIT_TOKEN}@github.com/%s .tmp_clone\n".formatted(repoPath));
        sb.append("  mv .tmp_clone/.git ./\n");
        sb.append("  cp -r .tmp_clone/* .\n");
        sb.append("  rm -rf .tmp_clone\n");
        sb.append("else\n");
        sb.append("  echo 'Репозиторий уже существует, выполняем git pull'\n");
        sb.append("  git pull\n");
        sb.append("fi\n\n");
        sb.append("echo 'Текущий коммит:'\n");
        sb.append("git log -1 --oneline || true\n\n");

        if (hasTerraform) {
            sb.append("if command -v terraform >/dev/null 2>&1; then\n");
            sb.append("  echo 'Terraform detected'\n");
            sb.append("  find . -type f -name \"*.tf\" | while read tf_file; do\n");
            sb.append("    dir=$(dirname \"$tf_file\")\n");
            sb.append("    echo \"→ Terraform apply в $dir\"\n");
            sb.append("    (cd \"$dir\" && terraform init && terraform apply -auto-approve)\n");
            sb.append("  done\n");
            sb.append("else\n");
            sb.append("  echo 'Terraform не установлен на сервере'\n");
            sb.append("  exit 1\n");
            sb.append("fi\n\n");
        }

        if (hasAnsible) {
            sb.append("if command -v ansible-playbook >/dev/null 2>&1; then\n");
            sb.append("  echo 'Ansible detected'\n");
            sb.append("  find . -type f \\( -name \"*.yml\" -o -name \"*.yaml\" \\) | while read f; do\n");
            sb.append("    if [[ \"$f\" == *\".github/workflows/\"* ]]; then\n");
            sb.append("      echo \"Пропуск служебного файла: $f\"\n");
            sb.append("      continue\n");
            sb.append("    fi\n");
            sb.append("    echo \"→ Запуск Ansible playbook: $f\"\n");
            sb.append("    ANSIBLE_HOST_KEY_CHECKING=False ansible-playbook \"$f\" -i localhost, -c local --extra-vars \"ansible_become_pass=$ANSIBLE_BECOME_PASS\" || echo \"⚠ Ошибка при запуске $f\"\n");
            sb.append("  done\n");
            sb.append("else\n");
            sb.append("  echo 'Ansible не установлен на сервере'\n");
            sb.append("  exit 1\n");
            sb.append("fi\n\n");
        }

        if (hasBash) {
            sb.append("echo 'Поиск и выполнение Bash-скриптов'\n");
            sb.append("find . -type f -name \"*.sh\" | while read sh_file; do\n");
            sb.append("  if [[ \"$sh_file\" == *\"/deploy.sh\" ]]; then\n");
            sb.append("    echo \"Пропуск самого deploy.sh\"\n");
            sb.append("    continue\n");
            sb.append("  fi\n");
            sb.append("  echo \"→ Запуск bash: $sh_file\"\n");
            sb.append("  bash \"$sh_file\" || echo \"⚠ Ошибка при выполнении $sh_file\"\n");
            sb.append("done\n\n");
        }

        if (!hasTerraform && !hasAnsible && !hasBash) {
            sb.append("echo 'Нет Terraform, Ansible или Bash-скриптов. Деплой пропущен.'\n");
            sb.append("exit 0\n");
        }

        sb.append("echo \"Деплой завершён\"\n");
        return sb.toString();
    }

    private String generateDeployYml(ApplicationDto app, ServersDto server) {
        String remoteDir = "/home/" + server.getSpecify_username() + "/apps/" + app.getName();
        return """
            name: Deploy %s

            on:
              push:
                branches:
                  - %s
              workflow_dispatch:
                inputs:
                  rollback:
                    description: 'Run rollback script manually'
                    required: false
                    default: 'false'

            jobs:
              deploy:
                if: ${{ github.event.inputs.rollback != 'true' }}
                runs-on: ubuntu-22.04
                env:
                  GIT_USERNAME: ${{ secrets.GIT_USERNAME }}
                  GIT_TOKEN: ${{ secrets.GIT_TOKEN }}
                  ANSIBLE_BECOME_PASS: ${{ secrets.SERVER_PASSWORD }}
                steps:
                  - name: Checkout
                    uses: actions/checkout@v3

                  - name: SSH Deploy
                    id: deploy_step
                    uses: appleboy/ssh-action@master
                    with:
                      host: %s
                      username: %s
                      password: ${{ secrets.SERVER_PASSWORD }}
                      envs: GIT_USERNAME,GIT_TOKEN,ANSIBLE_BECOME_PASS
                      script: |
                        cd %s
                        bash ./deploy.sh

              rollback:
                if: ${{ failure() && github.event.inputs.rollback != 'true' }}
                needs: deploy
                runs-on: ubuntu-22.04
                env:
                  GIT_USERNAME: ${{ secrets.GIT_USERNAME }}
                  GIT_TOKEN: ${{ secrets.GIT_TOKEN }}
                steps:
                  - name: Checkout
                    uses: actions/checkout@v3

                  - name: SSH Rollback
                    uses: appleboy/ssh-action@master
                    with:
                      host: %s
                      username: %s
                      password: ${{ secrets.SERVER_PASSWORD }}
                      envs: GIT_USERNAME,GIT_TOKEN
                      script: |
                        cd %s
                        bash ./rollback.sh
            """.formatted(
                app.getName(),
                app.getBranch(),
                server.getHost(),
                server.getSpecify_username(),
                remoteDir,
                server.getHost(),
                server.getSpecify_username(),
                remoteDir
        );
    }

    private String generateRollbackSh(ApplicationDto app) {
        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/bash\n");
        sb.append("set -e\n\n");
        sb.append("echo \"Начинается откат приложения %s...\"\n".formatted(app.getName()));
        sb.append("echo \"→ Terraform destroy (если есть)\"\n");
        sb.append("if command -v terraform >/dev/null 2>&1; then\n");
        sb.append("  find . -type f -name \"*.tf\" | while read tf_file; do\n");
        sb.append("    dir=$(dirname \"$tf_file\")\n");
        sb.append("    (cd \"$dir\" && terraform destroy -auto-approve || true)\n");
        sb.append("  done\n");
        sb.append("fi\n\n");
        sb.append("echo \"→ Git reset\"\n");
        sb.append("git reset --hard HEAD~1 || true\n");
        sb.append("echo \"Откат завершён\"\n");
        return sb.toString();
    }

    private void executeCommand(Session session, String command) throws Exception {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        channel.setInputStream(null);
        channel.setErrStream(System.err);
        channel.connect();

        try (OutputStream out = channel.getOutputStream()) {
            out.flush();
        }

        while (!channel.isClosed()) {
            Thread.sleep(100);
        }

        if (channel.getExitStatus() != 0) {
            throw new RuntimeException("Ошибка выполнения команды: " + command);
        }

        channel.disconnect();
    }

    public void generateGitflow(String appName) throws IOException, GitAPIException {
        ApplicationDto app = applicationService.getAll().stream()
                .filter(a -> a.getName().equals(appName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Приложение не найдено: " + appName));

        if (app.getRepoName() == null || app.getBranch() == null || app.getPath() == null) {
            throw new IllegalArgumentException("У приложения отсутствуют repoName, branch или path");
        }

        GitConnectionRequestDto repo = gitService.getByName(app.getRepoName());

        if (app.getServerName() == null || app.getServerName().isBlank()) {
            throw new IllegalArgumentException("Сервер не указан у приложения");
        }

        ServersDto server = serversService.getAll().stream()
                .filter(s -> s.getName().equals(app.getServerName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Сервер не найден: " + app.getServerName()));

        this.generate(app, server, repo);
    }

    private void uploadFilesToServer(String remoteDir, ServersDto server, Map<String, String> files) {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(server.getSpecify_username(), server.getHost(), server.getPort());
            session.setPassword(EncryptionUtils.decrypt(server.getPassword()));

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect(10000);

            executeCommand(session, "mkdir -p " + remoteDir);

            for (Map.Entry<String, String> entry : files.entrySet()) {
                String filename = entry.getKey();
                String content = entry.getValue();
                String remotePath = remoteDir + "/" + filename;
                String command = "cat > " + remotePath + " && chmod +x " + remotePath;

                ChannelExec channel = (ChannelExec) session.openChannel("exec");
                channel.setCommand(command);
                channel.setInputStream(null);
                channel.setErrStream(System.err);
                OutputStream out = channel.getOutputStream();
                channel.connect();

                out.write(content.getBytes(StandardCharsets.UTF_8));
                out.flush();
                out.close();

                while (!channel.isClosed()) {
                    Thread.sleep(100);
                }

                if (channel.getExitStatus() != 0) {
                    throw new RuntimeException("Ошибка при загрузке " + filename);
                }

                channel.disconnect();
            }

            session.disconnect();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при загрузке файлов на сервер: " + e.getMessage(), e);
        }
    }
}
