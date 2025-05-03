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

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;

@Service
@RequiredArgsConstructor
public class GitflowGeneratorService {

    private final GitWriterService gitWriterService;

    public void generate(ApplicationDto app, ServersDto server, GitConnectionRequestDto repo) throws IOException, GitAPIException {
        String appPath = app.getPath();
        String branch = app.getBranch();
        String remoteDir = "/home/" + server.getSpecify_username() + "/apps/" + app.getName();

        List<FileNodeDto> files = gitWriterService.listFiles(repo, branch, appPath);
        String deployScriptContent = generateDynamicDeploySh(app, server, files);

        gitWriterService.pushFile(
                repo,
                branch,
                appPath,
                "deploy.sh",
                deployScriptContent,
                "Добавлен автоматический deploy.sh"
        );

        String workflowContent = generateDeployYml(app, server);
        gitWriterService.pushFile(
                repo,
                branch,
                ".github/workflows",
                "deploy.yml",
                workflowContent,
                "Добавлен GitHub Actions workflow"
        );

        uploadDeployScriptToServer(remoteDir, server, deployScriptContent);
    }

    private String generateDynamicDeploySh(ApplicationDto app, ServersDto server, List<FileNodeDto> files) {
        String appDir = "/home/" + server.getSpecify_username() + "/apps/" + app.getName();

        boolean hasTerraform = files.stream().anyMatch(f -> f.getName().endsWith(".tf"));
        boolean hasAnsible = files.stream().anyMatch(f -> f.getName().endsWith(".yml") || f.getName().endsWith(".yaml"));

        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/bash\n");
        sb.append("set -e\n");
        sb.append("\n");
        sb.append("echo \"Начинается деплой %s...\"\n".formatted(app.getName()));
        sb.append("cd %s\n".formatted(appDir));
        sb.append("\n");

        if (hasTerraform) {
            sb.append("if command -v terraform >/dev/null 2>&1; then\n");
            sb.append("  echo 'Terraform detected'\n");
            sb.append("  terraform init\n");
            sb.append("  terraform apply -auto-approve\n");
            sb.append("else\n");
            sb.append("  echo 'Terraform не установлен на сервере'\n");
            sb.append("  exit 1\n");
            sb.append("fi\n\n");
        }

        if (hasAnsible) {
            sb.append("if command -v ansible-playbook >/dev/null 2>&1; then\n");
            sb.append("  echo 'Ansible detected'\n");
            sb.append("  ansible-playbook playbook.yml\n");
            sb.append("else\n");
            sb.append("  echo 'Ansible не установлен на сервере'\n");
            sb.append("  exit 1\n");
            sb.append("fi\n\n");
        }

        if (!hasTerraform && !hasAnsible) {
            sb.append("echo 'Ни одного поддерживаемого конфигурационного файла не найдено. Деплой пропущен.'\n");
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

                jobs:
                  deploy:
                    runs-on: ubuntu-22.04

                    steps:
                      - name: Checkout
                        uses: actions/checkout@v3

                      - name: SSH Deploy
                        uses: appleboy/ssh-action@master
                        with:
                          host: %s
                          username: %s
                          password: ${{ secrets.SERVER_PASSWORD }}
                          script: |
                            cd %s
                            bash ./deploy.sh
                """.formatted(
                app.getName(),
                app.getBranch(),
                server.getHost(),
                server.getSpecify_username(),
                remoteDir
        );
    }

    private void uploadDeployScriptToServer(String remoteDir, ServersDto server, String scriptContent) {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(server.getSpecify_username(), server.getHost(), server.getPort());
            session.setPassword(server.getPassword());

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect(10000);

            executeCommand(session, "mkdir -p " + remoteDir);

            String escaped = scriptContent.replace("\\", "\\\\")
                    .replace("$", "\\$")
                    .replace("\"", "\\\"")
                    .replace("`", "\\`");

            String command = "echo \"" + escaped + "\" > " + remoteDir + "/deploy.sh && chmod +x " + remoteDir + "/deploy.sh";
            executeCommand(session, command);

            session.disconnect();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при отправке deploy.sh на сервер: " + e.getMessage(), e);
        }
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
}