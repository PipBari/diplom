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
            return ResponseEntity.status(500).body("Ошибка при получении списка репозиториев: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> add(@RequestBody GitConnectionRequestDto request) {
        try {
            gitService.save(request);
            return ResponseEntity.status(201).body("Репозиторий успешно добавлен");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при добавлении репозитория: " + e.getMessage());
        }
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<?> delete(@PathVariable String name) {
        try {
            gitService.delete(name);
            return ResponseEntity.ok("Репозиторий успешно удалён");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("Репозиторий не найден: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при удалении репозитория: " + e.getMessage());
        }
    }

    @PostMapping("/{name}/status")
    public ResponseEntity<?> recheckStatus(@PathVariable String name) {
        try {
            String updatedStatus = gitService.recheckStatus(name);
            return ResponseEntity.ok("Статус обновлён: " + updatedStatus);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("Репозиторий не найден: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при проверке статуса: " + e.getMessage());
        }
    }

    @PutMapping("/{name}")
    public ResponseEntity<?> update(@PathVariable String name, @RequestBody GitConnectionRequestDto request) {
        try {
            gitService.update(name, request);
            return ResponseEntity.ok("Репозиторий успешно обновлён");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("Репозиторий не найден: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при обновлении репозитория: " + e.getMessage());
        }
    }
}
