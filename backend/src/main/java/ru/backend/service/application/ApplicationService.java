package ru.backend.service.application;

import org.springframework.stereotype.Service;
import ru.backend.rest.application.dto.ApplicationDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ApplicationService {

    private final List<ApplicationDto> applications = new ArrayList<>();

    public void addApplication(String name, String path) {
        String createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        applications.add(new ApplicationDto(name, path, createdAt));
    }

    public List<ApplicationDto> getAll() {
        return applications;
    }

    public void clearAll() {
        applications.clear();
    }
}
