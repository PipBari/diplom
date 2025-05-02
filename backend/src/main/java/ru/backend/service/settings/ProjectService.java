package ru.backend.service.settings;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.backend.rest.settings.dto.ProjectDto;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ProjectService {

    private final Map<String, ProjectDto> projectStorage = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final File storageFile;

    public ProjectService(@Value("${storage.base-dir}") String baseDirPath) {
        File baseDir = new File(resolvePath(baseDirPath));
        if (!baseDir.exists()) baseDir.mkdirs();
        this.storageFile = new File(baseDir, "projects.json");
    }

    private String resolvePath(String path) {
        return path.replace("${user.home}", System.getProperty("user.home"));
    }

    @PostConstruct
    public void loadFromDisk() {
        try {
            if (storageFile.exists()) {
                List<ProjectDto> list = objectMapper.readValue(storageFile, new TypeReference<>() {});
                for (ProjectDto dto : list) {
                    projectStorage.put(dto.getName(), dto);
                }
                log.info("Загружено {} проектов из файла", projectStorage.size());
            }
        } catch (Exception e) {
            log.error("Ошибка при загрузке проектов: ", e);
        }
    }

    @PreDestroy
    public void saveToDisk() {
        try {
            List<ProjectDto> list = new ArrayList<>(projectStorage.values());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storageFile, list);
            log.info("Сохранено {} проектов в файл", list.size());
        } catch (Exception e) {
            log.error("Ошибка при сохранении проектов: ", e);
        }
    }

    public List<ProjectDto> getAll() {
        return new ArrayList<>(projectStorage.values());
    }

    public void save(ProjectDto project) {
        projectStorage.put(project.getName(), project);
        saveToDisk();
    }

    public void delete(String name) {
        projectStorage.remove(name);
        saveToDisk();
    }
}
