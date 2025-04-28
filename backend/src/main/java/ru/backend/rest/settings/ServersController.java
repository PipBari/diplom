package ru.backend.rest.settings;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.backend.rest.settings.dto.ServersDto;
import ru.backend.service.settings.ServersService;

import java.util.List;

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
    public ResponseEntity<String> updateLoad(@PathVariable String name) {
        serverService.updateServerLoad(name);
        return ResponseEntity.ok("Нагрузка обновлена");
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<String> delete(@PathVariable String name) {
        serverService.delete(name);
        return ResponseEntity.ok("Сервер удалён");
    }
}
