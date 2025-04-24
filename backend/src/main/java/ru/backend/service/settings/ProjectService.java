package ru.backend.service.settings;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.backend.rest.settings.dto.ProjectDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ProjectService {

    private final Map<String, ProjectDto> projectStorage = new ConcurrentHashMap<>();

    public List<ProjectDto> getAll() {
        return new ArrayList<>(projectStorage.values());
    }

    public void save(ProjectDto project) {
        projectStorage.put(project.getName(), project);
    }

    public void delete(String name) {
        projectStorage.remove(name);
    }
}
