package ru.backend.rest.settings;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.backend.rest.settings.dto.ServersDto;
import ru.backend.service.settings.ServersService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@RestController
@RequestMapping("/settings/servers")
public class ServersController {

    private final ServersService serverService;

    public ServersController(ServersService serverService) {
        this.serverService = serverService;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        try {
            return ResponseEntity.ok(serverService.getAll());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка получения списка серверов: " + e.getMessage());
        }
    }

    @GetMapping("/{name}")
    public ResponseEntity<?> getByName(@PathVariable String name) {
        try {
            Optional<ServersDto> result = serverService.getAll().stream()
                    .filter(s -> s.getName().equals(name))
                    .findFirst();
            return result.<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(404).body("Сервер не найден: " + name));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при получении сервера: " + e.getMessage());
        }
    }

    @PutMapping("/{name}")
    public ResponseEntity<String> update(@PathVariable String name, @RequestBody ServersDto updated) {
        try {
            serverService.update(name, updated);
            return ResponseEntity.ok("Сервер обновлён");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("Сервер не найден: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Ошибка в аргументах: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при обновлении сервера: " + e.getMessage());
        }
    }

    @PostMapping("/{name}/status")
    public ResponseEntity<String> recheckStatus(@PathVariable String name) {
        try {
            String updatedStatus = serverService.recheckStatus(name);
            return ResponseEntity.ok("Статус обновлён: " + updatedStatus);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("Сервер не найден: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при обновлении статуса: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<String> add(@RequestBody ServersDto server) {
        try {
            serverService.save(server);
            return ResponseEntity.status(201).body("Сервер добавлен");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Ошибка в аргументах: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при добавлении сервера: " + e.getMessage());
        }
    }

    @PostMapping("/{name}/update-load")
    public ResponseEntity<?> updateLoad(@PathVariable String name) {
        try {
            serverService.updateServerLoad(name);
            Optional<ServersDto> updated = serverService.getAll().stream()
                    .filter(s -> s.getName().equals(name))
                    .findFirst();
            return updated.<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(404).body("Сервер не найден: " + name));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при обновлении загрузки сервера: " + e.getMessage());
        }
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<String> delete(@PathVariable String name) {
        try {
            serverService.delete(name);
            return ResponseEntity.ok("Сервер удалён");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("Сервер не найден: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при удалении сервера: " + e.getMessage());
        }
    }
}
