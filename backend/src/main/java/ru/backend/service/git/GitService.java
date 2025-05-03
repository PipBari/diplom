package ru.backend.service.git;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.backend.rest.git.dto.GitConnectionRequestDto;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class GitService {

    private final Map<String, GitConnectionRequestDto> repoStorage = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final File storageFile;

    public GitService(@Value("${storage.base-dir}") String baseDirPath) {
        File baseDir = new File(resolvePath(baseDirPath));
        if (!baseDir.exists()) baseDir.mkdirs();
        this.storageFile = new File(baseDir, "git-repos.json");
    }

    private String resolvePath(String path) {
        return path.replace("${user.home}", System.getProperty("user.home"));
    }

    @PostConstruct
    public void loadFromDisk() {
        try {
            if (storageFile.exists()) {
                List<GitConnectionRequestDto> list = objectMapper.readValue(
                        storageFile,
                        new TypeReference<>() {}
                );
                for (GitConnectionRequestDto dto : list) {
                    repoStorage.put(dto.getName(), dto);
                }
                log.info("Загружено {} репозиториев из файла", repoStorage.size());
            }
        } catch (Exception e) {
            log.error("Ошибка при загрузке git-репозиториев из файла: ", e);
        }
    }

    @PreDestroy
    public void saveToDisk() {
        try {
            List<GitConnectionRequestDto> list = new ArrayList<>(repoStorage.values());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storageFile, list);
            log.info("Сохранено {} репозиториев в файл", list.size());
        } catch (Exception e) {
            log.error("Ошибка при сохранении git-репозиториев в файл: ", e);
        }
    }

    public List<GitConnectionRequestDto> getAll() {
        return new ArrayList<>(repoStorage.values());
    }

    public void save(GitConnectionRequestDto request) {
        String status = checkConnection(request);
        request.setStatus(status);
        if (request.getType() == null || request.getType().isBlank()) {
            request.setType("git");
        }
        repoStorage.put(request.getName(), request);
        saveToDisk();
    }

    public void delete(String name) {
        repoStorage.remove(name);
        saveToDisk();
    }

    public String recheckStatus(String name) {
        GitConnectionRequestDto repo = repoStorage.get(name);
        if (repo == null) {
            return "Unknown";
        }
        String status = checkConnection(repo);
        repo.setStatus(status);
        saveToDisk();
        return status;
    }

    private String checkConnection(GitConnectionRequestDto request) {
        try {
            var tempDir = Files.createTempDirectory("git-check").toFile();
            CloneCommand cloneCommand = Git.cloneRepository()
                    .setURI(request.getRepoUrl())
                    .setBranch(request.getBranch())
                    .setDirectory(tempDir)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                            request.getUsername(),
                            request.getToken()
                    ))
                    .setCloneAllBranches(false)
                    .setNoCheckout(true);
            try (Git ignored = cloneCommand.call()) {
                return "Successful";
            }
        } catch (Exception e) {
            log.warn("Ошибка подключения к Git [{}]: {}", request.getRepoUrl(), e.getMessage());
            return "Error";
        }
    }

    public GitConnectionRequestDto getByName(String name) {
        GitConnectionRequestDto repo = repoStorage.get(name);
        if (repo == null) {
            throw new NoSuchElementException("Репозиторий не найден: " + name);
        }
        return repo;
    }

    public void update(String name, GitConnectionRequestDto updated) {
        GitConnectionRequestDto existing = repoStorage.get(name);
        if (existing == null) {
            throw new NoSuchElementException("Репозиторий не найден: " + name);
        }

        if (updated.getRepoUrl() != null) existing.setRepoUrl(updated.getRepoUrl());
        if (updated.getBranch() != null) existing.setBranch(updated.getBranch());
        if (updated.getUsername() != null) existing.setUsername(updated.getUsername());
        if (updated.getToken() != null) existing.setToken(updated.getToken());
        if (updated.getType() != null) existing.setType(updated.getType());

        String status = checkConnection(existing);
        existing.setStatus(status);

        repoStorage.put(name, existing);
        saveToDisk();
    }
}
