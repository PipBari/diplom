package ru.backend.service.git;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;
import ru.backend.rest.git.dto.FileNodeDto;
import ru.backend.rest.git.dto.GitConnectionRequestDto;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class GitWriterService {

    public boolean branchExists(GitConnectionRequestDto repo, String branch) {
        try {
            return Git.lsRemoteRepository()
                    .setRemote(repo.getRepoUrl())
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(repo.getUsername(), repo.getToken()))
                    .call()
                    .stream()
                    .anyMatch(ref -> ref.getName().endsWith("/" + branch));
        } catch (Exception e) {
            log.error("Ошибка при проверке ветки: {}", e.getMessage());
            return false;
        }
    }

    public boolean pathExists(GitConnectionRequestDto repo, String branch, String path) {
        if (path == null || path.isBlank() || ".".equals(path) || "/".equals(path)) {
            return false;
        }
        File tempDir = null;
        try {
            tempDir = Files.createTempDirectory("repo-check").toFile();
            Git git = Git.cloneRepository()
                    .setURI(repo.getRepoUrl())
                    .setBranch(branch)
                    .setDirectory(tempDir)
                    .setNoCheckout(false)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(repo.getUsername(), repo.getToken()))
                    .call();

            File targetPath = new File(tempDir, path);
            return targetPath.exists() && targetPath.isDirectory();

        } catch (Exception e) {
            log.error("Ошибка при проверке пути: {}", e.getMessage());
            return false;
        } finally {
            if (tempDir != null && tempDir.exists()) {
                deleteDirectory(tempDir);
            }
        }
    }

    public void pushFile(GitConnectionRequestDto repo, String branch, String path, String filename, String content, String commitMessage) throws IOException, GitAPIException {
        File tempDir = Files.createTempDirectory("repo-push").toFile();
        try (Git git = Git.cloneRepository()
                .setURI(repo.getRepoUrl())
                .setBranch(branch)
                .setDirectory(tempDir)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(repo.getUsername(), repo.getToken()))
                .call()) {

            File dir = new File(tempDir, path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) throw new IOException("Не удалось создать директорию " + path);
            }

            File file = new File(dir, filename);
            Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);

            git.add().addFilepattern(".").call();
            git.commit().setMessage(commitMessage).call();
            git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(repo.getUsername(), repo.getToken())).call();

        } finally {
            deleteDirectory(tempDir);
        }
    }

    public List<String> listFolders(GitConnectionRequestDto repo, String branch, String basePath) {
        List<String> result = new ArrayList<>();
        File tempDir = null;
        try {
            tempDir = Files.createTempDirectory("repo-list").toFile();
            Git.cloneRepository()
                    .setURI(repo.getRepoUrl())
                    .setBranch(branch)
                    .setDirectory(tempDir)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(repo.getUsername(), repo.getToken()))
                    .call();

            File dir = new File(tempDir, basePath);
            if (dir.exists() && dir.isDirectory()) {
                File[] folders = dir.listFiles(File::isDirectory);
                if (folders != null) {
                    Arrays.stream(folders).map(File::getName).forEach(result::add);
                }
            }

        } catch (Exception e) {
            log.error("Ошибка при получении списка папок: {}", e.getMessage());
        } finally {
            if (tempDir != null) deleteDirectory(tempDir);
        }
        return result;
    }

    private void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }

    public List<FileNodeDto> listFiles(GitConnectionRequestDto repo, String branch, String basePath) {
        List<FileNodeDto> result = new ArrayList<>();
        File tempDir = null;
        try {
            tempDir = Files.createTempDirectory("repo-list").toFile();
            Git.cloneRepository()
                    .setURI(repo.getRepoUrl())
                    .setBranch(branch)
                    .setDirectory(tempDir)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(repo.getUsername(), repo.getToken()))
                    .call();

            File dir = new File(tempDir, basePath);
            if (dir.exists() && dir.isDirectory()) {
                File[] contents = dir.listFiles();
                if (contents != null) {
                    for (File file : contents) {
                        result.add(new FileNodeDto(file.getName(), file.isDirectory() ? "folder" : "file"));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при получении файлов: {}", e.getMessage());
        } finally {
            if (tempDir != null) deleteDirectory(tempDir);
        }
        return result;
    }
}
