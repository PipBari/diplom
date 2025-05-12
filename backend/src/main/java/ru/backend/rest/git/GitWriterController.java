package ru.backend.rest.git;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.backend.rest.git.dto.*;
import ru.backend.rest.validation.dto.ValidationRequestDto;
import ru.backend.rest.validation.dto.ValidationResultDto;
import ru.backend.service.git.GitService;
import ru.backend.service.git.GitWriterService;
import ru.backend.service.validation.TemplateValidationService;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/git/writer")
@RequiredArgsConstructor
public class GitWriterController {

    private final GitWriterService gitWriterService;
    private final GitService gitService;
    private final TemplateValidationService validationService;

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
    public ResponseEntity<FileNodeDto> getEntry(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestParam String path
    ) {
        GitConnectionRequestDto repo = gitService.getByName(name);
        FileNodeDto entry = gitWriterService.readEntry(repo, branch, path);
        return ResponseEntity.ok(entry);
    }

    @PostMapping("/{name}/branch/{branch}/save")
    public ResponseEntity<ValidationResultDto> saveFile(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestBody GitFileSaveRequest request
    ) {
        GitConnectionRequestDto repo = gitService.getByName(name);

        try {
            String fullPath = request.getPath();
            String filename = fullPath.contains("/") ? fullPath.substring(fullPath.lastIndexOf('/') + 1) : fullPath;
            String folderPath = fullPath.contains("/") ? fullPath.substring(0, fullPath.lastIndexOf('/')) : "";

            String type = filename.endsWith(".tf") ? "terraform" :
                    (filename.endsWith(".yml") || filename.endsWith(".yaml")) ? "ansible" : null;

            if (type != null && request.getAllFiles() != null) {
                List<ValidationRequestDto> allFiles = new ArrayList<>(request.getAllFiles());

                allFiles.removeIf(f -> f.getFilename().equals(fullPath));
                allFiles.add(new ValidationRequestDto(
                        fullPath,
                        request.getContent(),
                        request.getServerName()
                ));

                ValidationResultDto result = validationService.validate(type, allFiles);
                if (!result.isValid()) {
                    return ResponseEntity.badRequest().body(result);
                }
            }

            gitWriterService.pushFile(
                    repo,
                    branch,
                    folderPath,
                    filename,
                    request.getContent(),
                    request.getCommitMessage()
            );

            return ResponseEntity.ok(new ValidationResultDto(true, "Файл сохранён в Git"));

        } catch (IOException | GitAPIException e) {
            return ResponseEntity.status(500)
                    .body(new ValidationResultDto(false, "Ошибка при сохранении: " + e.getMessage()));
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

    @PostMapping("/{name}/branch/{branch}/create-folder")
    public ResponseEntity<String> createFolder(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestBody GitCreateFolderRequest request
    ) {
        GitConnectionRequestDto repo = gitService.getByName(name);

        try {
            gitWriterService.createFolder(repo, branch, request.getPath(), request.getCommitMessage());
            return ResponseEntity.ok("Папка успешно создана");
        } catch (IOException | GitAPIException e) {
            return ResponseEntity.status(500).body("Ошибка при создании папки: " + e.getMessage());
        }
    }

    @DeleteMapping("/{name}/branch/{branch}/delete")
    public ResponseEntity<String> deletePath(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestParam String path,
            @RequestParam(defaultValue = "Удаление") String commitMessage
    ) {
        GitConnectionRequestDto repo = gitService.getByName(name);
        try {
            gitWriterService.deletePath(repo, branch, path, commitMessage);
            return ResponseEntity.ok("Удаление успешно выполнено");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при удалении: " + e.getMessage());
        }
    }

    @PutMapping("/{name}/branch/{branch}/rename")
    public ResponseEntity<String> renamePath(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestBody GitRenameRequest request
    ) {
        GitConnectionRequestDto repo = gitService.getByName(name);

        try {
            gitWriterService.renamePath(repo, branch, request.getOldPath(), request.getNewPath(), request.getCommitMessage());
            return ResponseEntity.ok("Переименование успешно выполнено");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при переименовании: " + e.getMessage());
        }
    }

    @PostMapping("/{name}/branch/{branch}/revert")
    public ResponseEntity<String> revertCommit(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestBody GitRevertRequest request
    ) {
        GitConnectionRequestDto repo = gitService.getByName(name);
        try {
            gitWriterService.revertCommit(repo, branch, request.getCommitHash(), request.getCommitMessage());
            return ResponseEntity.ok("Коммит успешно отменён");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при откате: " + e.getMessage());
        }
    }
}
