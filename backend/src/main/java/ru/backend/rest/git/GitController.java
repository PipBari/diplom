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
    public ResponseEntity<List<GitConnectionRequestDto>> getAll() {
        return ResponseEntity.ok(gitService.getAll());
    }

    @PostMapping
    public ResponseEntity<String> add(@RequestBody GitConnectionRequestDto request) {
        gitService.save(request);
        return ResponseEntity.ok("Репозиторий добавлен");
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<String> delete(@PathVariable String name) {
        gitService.delete(name);
        return ResponseEntity.ok("Репозиторий удалён");
    }

    @PostMapping("/{name}/status")
    public ResponseEntity<String> recheckStatus(@PathVariable String name) {
        String updatedStatus = gitService.recheckStatus(name);
        return ResponseEntity.ok("Статус обновлён: " + updatedStatus);
    }

    @PutMapping("/{name}")
    public ResponseEntity<String> update(@PathVariable String name, @RequestBody GitConnectionRequestDto request) {
        try {
            gitService.update(name, request);
            return ResponseEntity.ok("Репозиторий обновлён");
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
