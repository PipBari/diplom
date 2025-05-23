package ru.backend.rest.git;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.backend.rest.git.dto.*;
import ru.backend.rest.validation.dto.ValidationRequestDto;
import ru.backend.rest.validation.dto.ValidationResultDto;
import ru.backend.service.git.GitService;
import ru.backend.service.git.GitWriterService;
import ru.backend.service.validation.TemplateValidationService;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/git/writer")
@RequiredArgsConstructor
public class GitWriterController {

    private final GitWriterService gitWriterService;
    private final GitService gitService;
    private final TemplateValidationService validationService;

    @GetMapping("/{name}/branch/{branch}/exists")
    public ResponseEntity<?> branchExists(@PathVariable String name, @PathVariable String branch) {
        try {
            GitConnectionRequestDto repo = gitService.getByName(name);
            return ResponseEntity.ok(gitWriterService.branchExists(repo, branch));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка проверки ветки: " + e.getMessage());
        }
    }

    @GetMapping("/{name}/branch/{branch}/path/exists")
    public ResponseEntity<?> pathExists(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestParam String path
    ) {
        try {
            GitConnectionRequestDto repo = gitService.getByName(name);
            return ResponseEntity.ok(gitWriterService.pathExists(repo, branch, path));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка проверки пути: " + e.getMessage());
        }
    }

    @GetMapping("/{name}/branch/{branch}/folders")
    public ResponseEntity<?> listFolders(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestParam(defaultValue = "") String basePath
    ) {
        try {
            GitConnectionRequestDto repo = gitService.getByName(name);
            return ResponseEntity.ok(gitWriterService.listFolders(repo, branch, basePath));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка получения списка папок: " + e.getMessage());
        }
    }

    @GetMapping("/{name}/branch/{branch}/tree")
    public ResponseEntity<?> listFiles(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestParam(defaultValue = "") String path
    ) {
        try {
            GitConnectionRequestDto repo = gitService.getByName(name);
            return ResponseEntity.ok(gitWriterService.listFiles(repo, branch, path));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка получения списка файлов: " + e.getMessage());
        }
    }

    @GetMapping("/{name}/branch/{branch}/entry")
    public ResponseEntity<?> getEntry(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestParam String path
    ) {
        try {
            GitConnectionRequestDto repo = gitService.getByName(name);
            FileNodeDto entry = gitWriterService.readEntry(repo, branch, path);
            return ResponseEntity.ok(entry);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка получения элемента: " + e.getMessage());
        }
    }

    @PostMapping("/{name}/branch/{branch}/save")
    public ResponseEntity<ValidationResultDto> saveFile(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestBody GitFileSaveRequest request
    ) {
        try {
            GitConnectionRequestDto repo = gitService.getByName(name);
            ValidationResultDto result = gitWriterService.saveFileWithValidation(repo, branch, request, validationService);
            if (!result.isValid()) {
                return ResponseEntity.badRequest().body(result);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ValidationResultDto(false, "Ошибка при сохранении: " + e.getMessage()));
        }
    }

    @GetMapping("/{name}/branch/{branch}/file")
    public ResponseEntity<?> getFileContent(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestParam String path
    ) {
        try {
            GitConnectionRequestDto repo = gitService.getByName(name);
            String content = gitWriterService.getFileContent(repo, branch, path);
            return ResponseEntity.ok(content);
        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка чтения файла: " + e.getMessage());
        }
    }

    @GetMapping("/{name}/branch/{branch}/commits")
    public ResponseEntity<?> getCommits(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestParam String path,
            @RequestParam(defaultValue = "5") int limit
    ) {
        try {
            GitConnectionRequestDto repo = gitService.getByName(name);
            List<GitCommitDto> commits = gitWriterService.getRecentCommits(repo, branch, path, limit);
            return ResponseEntity.ok(commits);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка получения коммитов: " + e.getMessage());
        }
    }

    @PostMapping("/{name}/branch/{branch}/create-folder")
    public ResponseEntity<String> createFolder(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestBody GitCreateFolderRequest request
    ) {
        try {
            GitConnectionRequestDto repo = gitService.getByName(name);
            gitWriterService.createFolder(repo, branch, request.getPath(), request.getCommitMessage());
            return ResponseEntity.ok("Папка успешно создана");
        } catch (IOException | GitAPIException e) {
            return ResponseEntity.status(500).body("Ошибка при создании папки: " + e.getMessage());
        } catch (Exception e) {
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
        try {
            GitConnectionRequestDto repo = gitService.getByName(name);
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
        try {
            GitConnectionRequestDto repo = gitService.getByName(name);
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
        try {
            GitConnectionRequestDto repo = gitService.getByName(name);
            gitWriterService.revertCommit(repo, branch, request.getCommitHash(), request.getCommitMessage());
            return ResponseEntity.ok("Коммит успешно отменён");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при откате: " + e.getMessage());
        }
    }
}
