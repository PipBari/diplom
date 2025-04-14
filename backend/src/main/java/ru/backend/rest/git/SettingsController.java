package ru.backend.rest.git;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.backend.rest.git.dto.GitConnectionRequest;
import ru.backend.service.git.SettingsService;

@RestController
@RequestMapping("/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @PostMapping("/git")
    public ResponseEntity<String> saveGitSettings(@RequestBody GitConnectionRequest request) {
        settingsService.save(request);
        return ResponseEntity.ok("Настройки Git сохранены");
    }

    @GetMapping("/git")
    public ResponseEntity<GitConnectionRequest> getGitSettings() {
        if (!settingsService.isConfigured()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(settingsService.get());
    }
}
