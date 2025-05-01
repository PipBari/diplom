package ru.backend.rest.application;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.backend.rest.application.dto.ApplicationDto;
import ru.backend.service.application.ApplicationService;

import java.util.List;

@RestController
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public ResponseEntity<List<ApplicationDto>> getAll() {
        return ResponseEntity.ok(applicationService.getAll());
    }

    @PostMapping
    public ResponseEntity<String> add(@RequestBody ApplicationDto request) {
        applicationService.save(request);
        return ResponseEntity.ok("Приложение добавлено");
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<String> delete(@PathVariable String name) {
        applicationService.delete(name);
        return ResponseEntity.ok("Приложение удалено");
    }

    @PostMapping("/{name}/status")
    public ResponseEntity<String> recheckStatus(@PathVariable String name) {
        String status = applicationService.recheckStatus(name);
        return ResponseEntity.ok("Статус обновлён: " + status);
    }
}

