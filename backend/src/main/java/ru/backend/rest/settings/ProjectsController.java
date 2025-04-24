package ru.backend.rest.settings;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.backend.rest.settings.dto.ProjectDto;
import ru.backend.service.settings.ProjectService;

import java.util.List;

@RestController
@RequestMapping("/settings/projects")
public class ProjectsController {

    private final ProjectService projectService;

    public ProjectsController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ResponseEntity<List<ProjectDto>> getAll() {
        return ResponseEntity.ok(projectService.getAll());
    }

    @PostMapping
    public ResponseEntity<String> add(@RequestBody ProjectDto project) {
        projectService.save(project);
        return ResponseEntity.ok("Проект добавлен");
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<String> delete(@PathVariable String name) {
        projectService.delete(name);
        return ResponseEntity.ok("Проект удалён");
    }
}
