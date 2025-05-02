package ru.backend.service.git;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;
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
            Git.cloneRepository()
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

    public FileNodeDto readEntry(GitConnectionRequestDto repo, String branch, String path) {
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
                return new FileNodeDto("missing", "missing", "# Файл или папка не найдены", new ArrayList<>());
            }

            if (target.isDirectory()) {
                List<FileNodeDto> children = readChildrenRecursive(target);
                return new FileNodeDto(target.getName(), "folder", null, children);
            }

            String content = Files.readString(target.toPath(), StandardCharsets.UTF_8);
            return new FileNodeDto(target.getName(), "file", content, new ArrayList<>());

        } catch (Exception e) {
            log.error("Ошибка при чтении entry: {}", e.getMessage());
            return new FileNodeDto("error", "error", "# ошибка при чтении", new ArrayList<>());
        } finally {
            if (tempDir != null) deleteDirectory(tempDir);
        }
    }

    public List<FileNodeDto> listFiles(GitConnectionRequestDto repo, String branch, String basePath) {
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
                return readChildrenRecursive(dir);
            }

        } catch (Exception e) {
            log.error("Ошибка при получении файлов: {}", e.getMessage());
        } finally {
            if (tempDir != null) deleteDirectory(tempDir);
        }
        return new ArrayList<>();
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

    private List<FileNodeDto> readChildrenRecursive(File dir) {
        List<FileNodeDto> children = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) return children;

        for (File file : files) {
            if (file.isDirectory()) {
                children.add(new FileNodeDto(
                        file.getName(),
                        "folder",
                        null,
                        readChildrenRecursive(file)
                ));
            } else {
                children.add(new FileNodeDto(
                        file.getName(),
                        "file",
                        null,
                        new ArrayList<>()
                ));
            }
        }
        return children;
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
                        commit.getAuthorIdent().getWhen().toString(),
                        commit.getName()
                ));
            }

        } catch (Exception e) {
            log.error("Ошибка при получении коммитов: {}", e.getMessage());
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
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("Не удалось создать директорию " + folderPath);
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

    public void createFolder(GitConnectionRequestDto repo, String branch, String folderPath, String commitMessage) throws IOException, GitAPIException {
        File tempDir = Files.createTempDirectory("repo-folder").toFile();

        try (Git git = Git.cloneRepository()
                .setURI(repo.getRepoUrl())
                .setBranch(branch)
                .setDirectory(tempDir)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(repo.getUsername(), repo.getToken()))
                .call()) {

            File newFolder = new File(tempDir, folderPath);
            if (!newFolder.exists() && !newFolder.mkdirs()) {
                throw new IOException("Не удалось создать директорию: " + folderPath);
            }

            File gitkeep = new File(newFolder, ".gitkeep");
            Files.writeString(gitkeep.toPath(), "");

            git.add().addFilepattern(".").call();
            git.commit().setMessage(commitMessage).call();
            git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(repo.getUsername(), repo.getToken())).call();

        } finally {
            deleteDirectory(tempDir);
        }
    }

    public void deletePath(GitConnectionRequestDto repo, String branch, String path, String commitMessage) throws IOException, GitAPIException {
        File tempDir = Files.createTempDirectory("repo-delete").toFile();

        try (Git git = Git.cloneRepository()
                .setURI(repo.getRepoUrl())
                .setBranch(branch)
                .setDirectory(tempDir)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(repo.getUsername(), repo.getToken()))
                .call()) {

            File target = new File(tempDir, path);
            if (!target.exists()) {
                throw new IOException("Файл или папка не найдены: " + path);
            }

            deleteDirectory(target);

            git.rm().addFilepattern(path).call();
            git.commit().setMessage(commitMessage).call();
            git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(repo.getUsername(), repo.getToken())).call();

        } finally {
            deleteDirectory(tempDir);
        }
    }

    private void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) deleteDirectory(f);
        }
        dir.delete();
    }

    public void renamePath(GitConnectionRequestDto repo, String branch, String oldPath, String newPath, String commitMessage) throws IOException, GitAPIException {
        File tempDir = Files.createTempDirectory("repo-rename").toFile();

        try (Git git = Git.cloneRepository()
                .setURI(repo.getRepoUrl())
                .setBranch(branch)
                .setDirectory(tempDir)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(repo.getUsername(), repo.getToken()))
                .call()) {

            File oldFile = new File(tempDir, oldPath);
            File newFile = new File(tempDir, newPath);

            if (!oldFile.exists()) {
                throw new IOException("Исходный путь не существует: " + oldPath);
            }

            if (!oldFile.renameTo(newFile)) {
                throw new IOException("Не удалось переименовать: " + oldPath + " -> " + newPath);
            }

            git.rm().addFilepattern(oldPath).call();
            git.add().addFilepattern(newPath).call();
            git.commit().setMessage(commitMessage).call();
            git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(repo.getUsername(), repo.getToken())).call();

        } finally {
            deleteDirectory(tempDir);
        }
    }

    public void revertCommit(GitConnectionRequestDto repo, String branch, String commitHash, String commitMessage) throws IOException, GitAPIException {
        File tempDir = Files.createTempDirectory("repo-revert").toFile();

        try (Git git = Git.cloneRepository()
                .setURI(repo.getRepoUrl())
                .setBranch(branch)
                .setDirectory(tempDir)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(repo.getUsername(), repo.getToken()))
                .call()) {

            ObjectId commitId = git.getRepository().resolve(commitHash);
            git.revert()
                    .include(commitId)
                    .call();

            git.commit().setMessage(commitMessage).call();
            git.push()
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(repo.getUsername(), repo.getToken()))
                    .call();

        } finally {
            deleteDirectory(tempDir);
        }
    }
}
