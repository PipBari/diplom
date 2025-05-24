package ru.backend.rest.settings;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.backend.rest.settings.dto.ProjectDto;
import ru.backend.service.settings.ProjectService;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/settings/projects")
public class ProjectsController {

    private final ProjectService projectService;

    public ProjectsController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        try {
            List<ProjectDto> result = projectService.getAll();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка получения списка проектов: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<String> add(@RequestBody ProjectDto project) {
        try {
            projectService.save(project);
            return ResponseEntity.status(201).body("Проект добавлен");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Ошибка в аргументах: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при добавлении проекта: " + e.getMessage());
        }
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<String> delete(@PathVariable String name) {
        try {
            projectService.delete(name);
            return ResponseEntity.ok("Проект удалён");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("Проект не найден: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при удалении проекта: " + e.getMessage());
        }
    }

    @PutMapping("/{name}")
    public ResponseEntity<String> update(@PathVariable String name, @RequestBody ProjectDto project) {
        try {
            projectService.update(name, project);
            return ResponseEntity.ok("Проект обновлён");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("Проект не найден: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Ошибка в аргументах: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при обновлении проекта: " + e.getMessage());
        }
    }
}
