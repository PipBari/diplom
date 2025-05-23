package ru.backend.rest.git;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.backend.rest.git.dto.GitConnectionRequestDto;
import ru.backend.service.git.GitService;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/settings/git")
public class GitController {

    private final GitService gitService;

    public GitController(GitService gitService) {
        this.gitService = gitService;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        try {
            List<GitConnectionRequestDto> repos = gitService.getAll();
            return ResponseEntity.ok(repos);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка получения списка репозиториев: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<String> add(@RequestBody GitConnectionRequestDto request) {
        try {
            gitService.save(request);
            return ResponseEntity.ok("Репозиторий добавлен");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при добавлении репозитория: " + e.getMessage());
        }
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<String> delete(@PathVariable String name) {
        try {
            gitService.delete(name);
            return ResponseEntity.ok("Репозиторий удалён");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при удалении репозитория: " + e.getMessage());
        }
    }

    @PostMapping("/{name}/status")
    public ResponseEntity<String> recheckStatus(@PathVariable String name) {
        try {
            String updatedStatus = gitService.recheckStatus(name);
            return ResponseEntity.ok("Статус обновлён: " + updatedStatus);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при проверке статуса: " + e.getMessage());
        }
    }

    @PutMapping("/{name}")
    public ResponseEntity<String> update(@PathVariable String name, @RequestBody GitConnectionRequestDto request) {
        try {
            gitService.update(name, request);
            return ResponseEntity.ok("Репозиторий обновлён");
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().body("Репозиторий не найден: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при обновлении репозитория: " + e.getMessage());
        }
    }
}
