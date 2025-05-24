package ru.backend.rest.application;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.backend.rest.application.dto.ApplicationDto;
import ru.backend.service.application.ApplicationService;

import java.util.List;
import java.util.NoSuchElementException;

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
            return ResponseEntity.status(500).body("Ошибка сервера при получении приложений: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> add(@RequestBody ApplicationDto request) {
        try {
            if (request.getName() == null || request.getName().isBlank()) {
                return ResponseEntity.badRequest().body("Имя приложения не может быть пустым");
            }
            applicationService.save(request);
            return ResponseEntity.status(201).body("Приложение успешно добавлено");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Неверные данные: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка сервера при добавлении: " + e.getMessage());
        }
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<?> delete(@PathVariable String name) {
        try {
            applicationService.delete(name);
            return ResponseEntity.ok("Приложение удалено");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("Приложение не найдено: " + name);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка сервера при удалении: " + e.getMessage());
        }
    }

    @PostMapping("/{name}/status")
    public ResponseEntity<?> recheckStatus(@PathVariable String name) {
        try {
            String status = applicationService.recheckStatus(name);
            return ResponseEntity.ok("Статус обновлён: " + status);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("Приложение не найдено: " + name);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка сервера при обновлении статуса: " + e.getMessage());
        }
    }

    @PutMapping("/{name}")
    public ResponseEntity<?> update(@PathVariable String name, @RequestBody ApplicationDto request) {
        try {
            applicationService.update(name, request);
            return ResponseEntity.ok("Приложение обновлено");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("Приложение не найдено: " + name);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при обновлении: " + e.getMessage());
        }
    }
}
