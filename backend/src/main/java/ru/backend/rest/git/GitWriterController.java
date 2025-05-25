package ru.backend.rest.git;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.backend.rest.git.dto.*;
import ru.backend.rest.validation.dto.ValidationResultDto;
import ru.backend.service.application.ApplicationService;
import ru.backend.service.git.GitService;
import ru.backend.service.git.GitWriterService;
import ru.backend.service.validation.TemplateValidationService;
import ru.backend.util.EncryptionUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/git/writer")
@RequiredArgsConstructor
public class GitWriterController {

    private final GitWriterService gitWriterService;
    private final GitService gitService;
    private final TemplateValidationService validationService;
    private final ApplicationService applicationService;

    @GetMapping("/{name}/branch/{branch}/exists")
    public ResponseEntity<?> branchExists(@PathVariable String name, @PathVariable String branch) {
        try {
            return ResponseEntity.ok(gitWriterService.branchExists(gitService.getByName(name), branch));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("Репозиторий не найден: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка проверки ветки: " + e.getMessage());
        }
    }

    @GetMapping("/{name}/branch/{branch}/path/exists")
    public ResponseEntity<?> pathExists(@PathVariable String name, @PathVariable String branch, @RequestParam String path) {
        try {
            return ResponseEntity.ok(gitWriterService.pathExists(gitService.getByName(name), branch, path));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("Репозиторий не найден: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка проверки пути: " + e.getMessage());
        }
    }

    @GetMapping("/{name}/branch/{branch}/folders")
    public ResponseEntity<?> listFolders(@PathVariable String name, @PathVariable String branch, @RequestParam(defaultValue = "") String basePath) {
        try {
            return ResponseEntity.ok(gitWriterService.listFolders(gitService.getByName(name), branch, basePath));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("Репозиторий не найден: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка получения списка папок: " + e.getMessage());
        }
    }

    @GetMapping("/{name}/branch/{branch}/tree")
    public ResponseEntity<?> listFiles(@PathVariable String name, @PathVariable String branch, @RequestParam(defaultValue = "") String path) {
        try {
            return ResponseEntity.ok(gitWriterService.listFiles(gitService.getByName(name), branch, path));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("Репозиторий не найден: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка получения списка файлов: " + e.getMessage());
        }
    }

    @GetMapping("/{name}/branch/{branch}/entry")
    public ResponseEntity<?> getEntry(@PathVariable String name, @PathVariable String branch, @RequestParam String path) {
        try {
            return ResponseEntity.ok(gitWriterService.readEntry(gitService.getByName(name), branch, path));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("Репозиторий не найден: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка получения элемента: " + e.getMessage());
        }
    }

    @PostMapping("/{name}/branch/{branch}/save")
    public ResponseEntity<ValidationResultDto> saveFile(@PathVariable String name, @PathVariable String branch, @RequestBody GitFileSaveRequest request) {
        try {
            ValidationResultDto result = gitWriterService.saveFileWithValidation(gitService.getByName(name), branch, request, validationService);
            if (!result.isValid()) {
                return ResponseEntity.badRequest().body(result);
            }
            return ResponseEntity.ok(result);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(new ValidationResultDto(false, "Репозиторий не найден: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ValidationResultDto(false, "Ошибка при сохранении: " + e.getMessage()));
        }
    }

    @PostMapping("/branches")
    public ResponseEntity<List<String>> listBranches(@RequestBody Map<String, String> raw) {
        try {
            GitConnectionRequestDto dto = new GitConnectionRequestDto();
            dto.setRepoUrl(raw.get("url"));
            dto.setUsername(raw.get("username"));
            dto.setToken(raw.get("token"));
            return ResponseEntity.ok(gitWriterService.listBranches(dto));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of("Ошибка: " + e.getMessage()));
        }
    }

    @PostMapping("/repo/branch")
    public ResponseEntity<String> createBranch(@RequestBody GitBranchCreateRequest request) {
        try {
            gitWriterService.createBranch(request.toConnectionDto(), request.getName(), request.getFrom());
            return ResponseEntity.ok("Ветка создана: " + request.getName());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при создании ветки: " + e.getMessage());
        }
    }

    @DeleteMapping("/repo/branch")
    public ResponseEntity<String> deleteBranch(@RequestBody GitBranchDeleteRequest request) {
        try {
            gitWriterService.deleteBranch(request.toConnectionDto(), request.getBranch());
            return ResponseEntity.ok("Ветка удалена: " + request.getBranch());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при удалении ветки: " + e.getMessage());
        }
    }

    @GetMapping("/{name}/branch/{branch}/file")
    public ResponseEntity<?> getFileContent(@PathVariable String name, @PathVariable String branch, @RequestParam String path) {
        try {
            return ResponseEntity.ok(gitWriterService.getFileContent(gitService.getByName(name), branch, path));
        } catch (FileNotFoundException e) {
            return ResponseEntity.status(404).body("Файл не найден: " + e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("Репозиторий не найден: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка чтения файла: " + e.getMessage());
        }
    }

    @GetMapping("/{name}/branch/{branch}/commits")
    public ResponseEntity<?> getCommits(@PathVariable String name, @PathVariable String branch, @RequestParam String path, @RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "5") int limit) {
        try {
            return ResponseEntity.ok(
                    gitWriterService.getRecentCommits(
                            gitService.getByName(name), branch, path, offset, limit
                    )
            );
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("Репозиторий не найден: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка получения коммитов: " + e.getMessage());
        }
    }

    @PostMapping("/{name}/branch/{branch}/create-folder")
    public ResponseEntity<String> createFolder(@PathVariable String name, @PathVariable String branch, @RequestBody GitCreateFolderRequest request) {
        try {
            gitWriterService.createFolder(gitService.getByName(name), branch, request.getPath(), request.getCommitMessage());
            return ResponseEntity.status(201).body("Папка успешно создана");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("Репозиторий не найден: " + e.getMessage());
        } catch (IOException | GitAPIException e) {
            return ResponseEntity.status(500).body("Ошибка при создании папки: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при создании папки: " + e.getMessage());
        }
    }

    @DeleteMapping("/{name}/branch/{branch}/delete")
    public ResponseEntity<String> deletePath(@PathVariable String name, @PathVariable String branch, @RequestParam String path, @RequestParam(defaultValue = "Удаление") String commitMessage) {
        try {
            gitWriterService.deletePath(gitService.getByName(name), branch, path, commitMessage);
            return ResponseEntity.ok("Удаление успешно выполнено");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("Репозиторий не найден: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при удалении: " + e.getMessage());
        }
    }

    @PutMapping("/{name}/branch/{branch}/rename")
    public ResponseEntity<String> renamePath(@PathVariable String name, @PathVariable String branch, @RequestBody GitRenameRequest request) {
        try {
            gitWriterService.renamePath(gitService.getByName(name), branch, request.getOldPath(), request.getNewPath(), request.getCommitMessage());
            return ResponseEntity.ok("Переименование успешно выполнено");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("Репозиторий не найден: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при переименовании: " + e.getMessage());
        }
    }

    @PostMapping("/{name}/branch/{branch}/revert")
    public ResponseEntity<String> revertCommit(@PathVariable String name, @PathVariable String branch, @RequestBody GitRevertRequest request) {
        try {
            gitWriterService.revertCommit(gitService.getByName(name), branch, request.getCommitHash(), request.getCommitMessage());
            return ResponseEntity.ok("Коммит успешно отменён");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("Репозиторий не найден: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при откате: " + e.getMessage());
        }
    }

    @GetMapping("/{name}/branch/{branch}/diff")
    public ResponseEntity<?> getCommitDiff(
            @PathVariable String name,
            @PathVariable String branch,
            @RequestParam String commitHash,
            @RequestParam String path) {
        try {
            GitConnectionRequestDto repo = gitService.getByName(name);
            String diff = gitWriterService.getCommitDiff(repo, branch, commitHash, path);
            return ResponseEntity.ok(diff);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Репозиторий не найден: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Ошибка при получении diff: " + e.getMessage()));
        }
    }

    @GetMapping("/{name}/branch/{branch}/archive")
    public ResponseEntity<byte[]> downloadArchive(@PathVariable String name, @PathVariable String branch) {
        try {
            GitConnectionRequestDto repo = gitService.getByName(name);
            byte[] zip = gitWriterService.downloadArchive(repo, branch);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + name + "-" + branch + ".zip\"")
                    .header("Content-Type", "application/zip")
                    .body(zip);

        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(("Репозиторий не найден: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(("Ошибка при создании архива: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }
}
