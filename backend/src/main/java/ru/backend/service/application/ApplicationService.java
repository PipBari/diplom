package ru.backend.service.application;

import org.springframework.stereotype.Service;
import ru.backend.rest.application.dto.ApplicationDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ApplicationService {

    private final Map<String, ApplicationDto> applicationStorage = new ConcurrentHashMap<>();

    public List<ApplicationDto> getAll() {
        return new ArrayList<>(applicationStorage.values());
    }

    public void save(ApplicationDto app) {
        if (app.getCreatedAt() == null || app.getCreatedAt().isBlank()) {
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            app.setCreatedAt(now);
        }
        if (app.getStatus() == null) {
            app.setStatus("Not Synced");
        }
        applicationStorage.put(app.getName(), app);
    }

    public void delete(String name) {
        applicationStorage.remove(name);
    }

    public String recheckStatus(String name) {
        ApplicationDto app = applicationStorage.get(name);
        if (app == null) return "Unknown";
        app.setStatus("Checked");
        return "Checked";
    }
}

