package ru.backend.rest.git;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.backend.rest.git.dto.*;
import ru.backend.service.git.GitService;
import ru.backend.service.git.GitWriterService;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/git/writer")
@RequiredArgsConstructor
public class GitWriterController {

    private final GitWriterService gitWriterService;
    private final GitService gitService;

    @GetMapping("/{name}/branch/{branch}/exists")
    public ResponseEntity<Boolean> branchExists(@PathVariable String name, @PathVariable String branch) {
        GitConnectionRequestDto repo = gitService.getByName(name);
        return ResponseEntity.ok(gitWriterService.branchExists(repo, branch));
    }

    @GetMapping("/{name}/branch/{branch}/path/exists")
    public ResponseEntity<Boolean> pathExists(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestParam String path
    ) {
        GitConnectionRequestDto repo = gitService.getByName(name);
        return ResponseEntity.ok(gitWriterService.pathExists(repo, branch, path));
    }

    @GetMapping("/{name}/branch/{branch}/folders")
    public ResponseEntity<List<String>> listFolders(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestParam(defaultValue = "") String basePath
    ) {
        GitConnectionRequestDto repo = gitService.getByName(name);
        return ResponseEntity.ok(gitWriterService.listFolders(repo, branch, basePath));
    }

    @GetMapping("/{name}/branch/{branch}/tree")
    public ResponseEntity<List<FileNodeDto>> listFiles(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestParam(defaultValue = "") String path
    ) {
        GitConnectionRequestDto repo = gitService.getByName(name);
        return ResponseEntity.ok(gitWriterService.listFiles(repo, branch, path));
    }

    @GetMapping("/{name}/branch/{branch}/entry")
    public ResponseEntity<FileEntryDto> getEntry(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestParam String path
    ) {
        GitConnectionRequestDto repo = gitService.getByName(name);
        FileEntryDto entry = gitWriterService.readEntry(repo, branch, path);
        return ResponseEntity.ok(entry);
    }

    @PostMapping("/{name}/branch/{branch}/save")
    public ResponseEntity<String> saveFile(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestBody GitFileSaveRequest request
    ) {
        GitConnectionRequestDto repo = gitService.getByName(name);
        try {
            String fullPath = request.getPath();
            String filename = fullPath.contains("/") ? fullPath.substring(fullPath.lastIndexOf('/') + 1) : fullPath;
            String folderPath = fullPath.contains("/") ? fullPath.substring(0, fullPath.lastIndexOf('/')) : "";

            gitWriterService.pushFile(
                    repo,
                    branch,
                    folderPath,
                    filename,
                    request.getContent(),
                    request.getCommitMessage()
            );
            return ResponseEntity.ok("Файл сохранён в Git");
        } catch (IOException | GitAPIException e) {
            return ResponseEntity.status(500).body("Ошибка при сохранении: " + e.getMessage());
        }
    }

    @GetMapping("/{name}/branch/{branch}/file")
    public ResponseEntity<String> getFileContent(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestParam String path
    ) {
        GitConnectionRequestDto repo = gitService.getByName(name);
        File tempDir = null;
        try {
            tempDir = Files.createTempDirectory("repo-read").toFile();
            Git.cloneRepository()
                    .setURI(repo.getRepoUrl())
                    .setBranch(branch)
                    .setDirectory(tempDir)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(repo.getUsername(), repo.getToken()))
                    .call();

            File targetFile = new File(tempDir, path);
            if (!targetFile.exists() || targetFile.isDirectory()) {
                return ResponseEntity.notFound().build();
            }

            String content = Files.readString(targetFile.toPath(), StandardCharsets.UTF_8);
            return ResponseEntity.ok(content);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка чтения файла: " + e.getMessage());
        } finally {
            if (tempDir != null) {
                System.out.println("Временная папка не удалена: " + tempDir.getAbsolutePath());
            }
        }
    }

    @GetMapping("/{name}/branch/{branch}/commits")
    public ResponseEntity<List<GitCommitDto>> getCommits(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestParam String path,
            @RequestParam(defaultValue = "5") int limit
    ) {
        GitConnectionRequestDto repo = gitService.getByName(name);
        List<GitCommitDto> commits = gitWriterService.getRecentCommits(repo, branch, path, limit);
        return ResponseEntity.ok(commits);
    }
}
