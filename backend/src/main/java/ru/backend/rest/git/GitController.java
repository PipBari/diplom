package ru.backend.rest.git;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.backend.rest.git.dto.GitConnectionRequest;
import ru.backend.service.git.GitService;

import java.util.List;

@RestController
@RequestMapping("/settings/git")
public class GitController {

    private final GitService gitService;

    public GitController(GitService gitService) {
        this.gitService = gitService;
    }

    @GetMapping
    public ResponseEntity<List<GitConnectionRequest>> getAll() {
        return ResponseEntity.ok(gitService.getAll());
    }

    @PostMapping
    public ResponseEntity<String> add(@RequestBody GitConnectionRequest request) {
        gitService.save(request);
        return ResponseEntity.ok("Репозиторий добавлен");
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<String> delete(@PathVariable String name) {
        gitService.delete(name);
        return ResponseEntity.ok("Репозиторий удалён");
    }

    @PostMapping("/{name}/status")
    public ResponseEntity<String> updateStatus(@PathVariable String name, @RequestBody String status) {
        gitService.updateStatus(name, status);
        return ResponseEntity.ok("Статус обновлён");
    }
}