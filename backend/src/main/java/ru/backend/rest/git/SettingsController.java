package ru.backend.rest.git;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settings")
public class SettingsController {

    @GetMapping
    public ResponseEntity<String> getSettingsPage() {
        return ResponseEntity.ok("Настройки доступны. Перейдите на /settings/git для управления Git-репозиториями.");
    }
}