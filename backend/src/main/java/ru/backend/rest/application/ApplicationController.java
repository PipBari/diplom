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
    public ResponseEntity<?> getAll() {
        try {
            List<ApplicationDto> result = applicationService.getAll();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка получения приложений: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<String> add(@RequestBody ApplicationDto request) {
        try {
            applicationService.save(request);
            return ResponseEntity.ok("Приложение добавлено");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при добавлении: " + e.getMessage());
        }
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<String> delete(@PathVariable String name) {
        try {
            applicationService.delete(name);
            return ResponseEntity.ok("Приложение удалено");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при удалении: " + e.getMessage());
        }
    }

    @PostMapping("/{name}/status")
    public ResponseEntity<String> recheckStatus(@PathVariable String name) {
        try {
            String status = applicationService.recheckStatus(name);
            return ResponseEntity.ok("Статус обновлён: " + status);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка обновления статуса: " + e.getMessage());
        }
    }
}
