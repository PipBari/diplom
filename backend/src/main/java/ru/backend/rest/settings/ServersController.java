package ru.backend.rest.settings;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.backend.rest.settings.dto.ServersDto;
import ru.backend.service.settings.ServersService;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/settings/servers")
public class ServersController {

    private final ServersService serverService;

    public ServersController(ServersService serverService) {
        this.serverService = serverService;
    }

    @GetMapping
    public ResponseEntity<List<ServersDto>> getAll() {
        return ResponseEntity.ok(serverService.getAll());
    }

    @GetMapping("/{name}")
    public ResponseEntity<ServersDto> getByName(@PathVariable String name) {
        return serverService.getAll().stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{name}")
    public ResponseEntity<String> update(@PathVariable String name, @RequestBody ServersDto updated) {
        try {
            serverService.update(name, updated);
            return ResponseEntity.ok("Сервер обновлён");
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{name}/status")
    public ResponseEntity<String> recheckStatus(@PathVariable String name) {
        String updatedStatus = serverService.recheckStatus(name);
        return ResponseEntity.ok("Статус обновлён: " + updatedStatus);
    }

    @PostMapping
    public ResponseEntity<String> add(@RequestBody ServersDto server) {
        serverService.save(server);
        return ResponseEntity.ok("Сервер добавлен");
    }

    @PostMapping("/{name}/update-load")
    public ResponseEntity<ServersDto> updateLoad(@PathVariable String name) {
        serverService.updateServerLoad(name);
        return serverService.getAll().stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<String> delete(@PathVariable String name) {
        serverService.delete(name);
        return ResponseEntity.ok("Сервер удалён");
    }
}
