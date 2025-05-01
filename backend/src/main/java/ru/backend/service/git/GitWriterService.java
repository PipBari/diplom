package ru.backend.service.git;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;
import ru.backend.rest.git.dto.FileEntryDto;
import ru.backend.rest.git.dto.FileNodeDto;
import ru.backend.rest.git.dto.GitCommitDto;
import ru.backend.rest.git.dto.GitConnectionRequestDto;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

    public List<GitCommitDto> getRecentCommits(GitConnectionRequestDto repo, String branch, String path, int limit) {
        List<GitCommitDto> result = new ArrayList<>();
        File tempDir = null;
        try {
            tempDir = Files.createTempDirectory("repo-log").toFile();
            Git git = Git.cloneRepository()
                    .setURI(repo.getRepoUrl())
                    .setBranch(branch)
                    .setDirectory(tempDir)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(repo.getUsername(), repo.getToken()))
                    .call();

            Iterable<RevCommit> logs = git.log()
                    .addPath(path)
                    .setMaxCount(limit)
                    .call();

            for (RevCommit commit : logs) {
                result.add(new GitCommitDto(
                        commit.getAuthorIdent().getName(),
                        commit.getShortMessage(),
                        commit.getAuthorIdent().getWhen().toString()
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (tempDir != null) {
                System.out.println("Временная папка не удалена (log): " + tempDir.getAbsolutePath());
            }
        }

        return result;
    }

    public void pushFile(GitConnectionRequestDto repo, String branch, String folderPath, String filename, String content, String commitMessage) throws IOException, GitAPIException {
        File tempDir = Files.createTempDirectory("repo-push").toFile();
        try (Git git = Git.cloneRepository()
                .setURI(repo.getRepoUrl())
                .setBranch(branch)
                .setDirectory(tempDir)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(repo.getUsername(), repo.getToken()))
                .call()) {

            File dir = new File(tempDir, folderPath);
            if (!dir.exists()) {
                if (!dir.mkdirs()) throw new IOException("Не удалось создать директорию " + folderPath);
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

    public FileEntryDto readEntry(GitConnectionRequestDto repo, String branch, String path) {
        File tempDir = null;
        try {
            tempDir = Files.createTempDirectory("repo-entry").toFile();
            Git.cloneRepository()
                    .setURI(repo.getRepoUrl())
                    .setBranch(branch)
                    .setDirectory(tempDir)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(repo.getUsername(), repo.getToken()))
                    .call();

            File target = new File(tempDir, path);
            if (!target.exists()) {
                return new FileEntryDto("missing", target.getName(), "# Файл или папка не найдены", null);
            }

            if (target.isDirectory()) {
                File[] files = target.listFiles();
                List<FileNodeDto> children = new ArrayList<>();
                if (files != null) {
                    for (File f : files) {
                        children.add(new FileNodeDto(f.getName(), f.isDirectory() ? "folder" : "file"));
                    }
                }
                return new FileEntryDto("folder", target.getName(), null, children);
            }

            String content = Files.readString(target.toPath(), StandardCharsets.UTF_8);
            return new FileEntryDto("file", target.getName(), content, null);

        } catch (Exception e) {
            log.error("Ошибка при чтении entry: {}", e.getMessage());
            return new FileEntryDto("error", "", "# ошибка при чтении", null);
        } finally {
            if (tempDir != null) {
                deleteDirectory(tempDir);
            }
        }
    }
}
