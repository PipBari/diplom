package ru.backend.rest.git;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.backend.rest.git.dto.GitConnectionRequest;
import ru.backend.service.git.GitService;
import ru.backend.service.git.SettingsService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/git")
public class GitController {

    private final GitService gitService;
    private final SettingsService settingsService;

    public GitController(GitService gitService, SettingsService settingsService) {
        this.gitService = gitService;
        this.settingsService = settingsService;
    }

    @PostMapping("/sync")
    public ResponseEntity<String> syncRepo(@RequestBody GitConnectionRequest request) {
        try {
            gitService.cloneOrUpdateRepository(
                    request.getRepoUrl(),
                    request.getLocalPath(),
                    request.getBranch(),
                    request.getUsername(),
                    request.getToken()
            );
            return ResponseEntity.ok("Репозиторий " + request.getRepoUrl() + " синхронизирован на ветке " + request.getBranch());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка: " + e.getMessage());
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getCommitHistory(@RequestParam(defaultValue = "10") int limit) {
        if (!settingsService.isConfigured()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Git настройки не заданы");
        }

        try {
            String localPath = settingsService.get().getLocalPath();
            List<String> commits = gitService.getCommitHistory(localPath, limit);
            return ResponseEntity.ok(commits);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Ошибка: " + e.getMessage());
        } catch (GitAPIException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка Git: " + e.getMessage());
        }
    }

    @GetMapping("/history/last-commit")
    public ResponseEntity<String> getLastCommit() {
        if (!settingsService.isConfigured()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Git настройки не заданы");
        }

        try {
            String localPath = settingsService.get().getLocalPath();
            String commit = gitService.getLastCommit(localPath);
            return ResponseEntity.ok(commit);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Ошибка: " + e.getMessage());
        } catch (GitAPIException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка Git: " + e.getMessage());
        }
    }
}
