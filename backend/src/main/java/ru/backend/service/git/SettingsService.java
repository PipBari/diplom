package ru.backend.service.git;

import org.springframework.stereotype.Service;
import ru.backend.rest.git.dto.GitConnectionRequest;

@Service
public class SettingsService {

    private GitConnectionRequest settings;

    public void save(GitConnectionRequest request) {
        this.settings = request;
    }

    public GitConnectionRequest get() {
        return this.settings;
    }

    public boolean isConfigured() {
        return this.settings != null;
    }
}
