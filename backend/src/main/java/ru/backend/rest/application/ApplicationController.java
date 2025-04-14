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
    public ResponseEntity<List<ApplicationDto>> getAllApplications() {
        return ResponseEntity.ok(applicationService.getAll());
    }

    @PostMapping
    public ResponseEntity<String> createApplication(@RequestBody ApplicationDto request) {
        applicationService.addApplication(request.getName(), request.getPath());
        return ResponseEntity.ok("Приложение добавлено");
    }
}
