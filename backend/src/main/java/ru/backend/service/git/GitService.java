package ru.backend.service.git;

import org.springframework.stereotype.Service;
import ru.backend.rest.git.dto.GitConnectionRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GitService {

    private final List<GitConnectionRequest> repositories = new ArrayList<>();

    public void save(GitConnectionRequest request) {
        repositories.add(request);
    }

    public List<GitConnectionRequest> getAll() {
        return repositories;
    }

    public Optional<GitConnectionRequest> getByName(String name) {
        return repositories.stream()
                .filter(r -> r.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public void delete(String name) {
        repositories.removeIf(r -> r.getName().equalsIgnoreCase(name));
    }

    public void updateStatus(String name, String newStatus) {
        getByName(name).ifPresent(r -> r.setStatus(newStatus));
    }

    public boolean isConfigured() {
        return !repositories.isEmpty();
    }

    public void clearAll() {
        repositories.clear();
    }
}