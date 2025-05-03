package ru.backend.service.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.backend.rest.application.dto.ApplicationDto;
import ru.backend.rest.git.dto.GitConnectionRequestDto;
import ru.backend.rest.ws.WebSocketStatusController;
import ru.backend.service.git.GitService;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ApplicationService {

    private final GitService gitService;
    private final WebSocketStatusController statusController;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final File storageFile;

    private final Map<String, ApplicationDto> applicationStorage = new ConcurrentHashMap<>();

    public ApplicationService(GitService gitService,
                              WebSocketStatusController statusController,
                              @Value("${storage.base-dir}") String baseDirPath) {
        this.gitService = gitService;
        this.statusController = statusController;
        File baseDir = new File(resolvePath(baseDirPath));
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        this.storageFile = new File(baseDir, "applications.json");
    }

    private String resolvePath(String path) {
        return path.replace("${user.home}", System.getProperty("user.home"));
    }

    @PostConstruct
    public void loadFromDisk() {
        try {
            if (storageFile.exists()) {
                List<ApplicationDto> list = objectMapper.readValue(storageFile, new TypeReference<>() {});
                for (ApplicationDto dto : list) {
                    applicationStorage.put(dto.getName(), dto);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void saveToDisk() {
        try {
            List<ApplicationDto> list = new ArrayList<>(applicationStorage.values());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storageFile, list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
        saveToDisk();
    }

    public void delete(String name) {
        applicationStorage.remove(name);
        saveToDisk();
    }

    public String recheckStatus(String name) {
        ApplicationDto app = applicationStorage.get(name);
        if (app == null) return "Unknown";

        try {
            GitConnectionRequestDto repo = gitService.getByName(app.getRepoName());

            File tempDir = Files.createTempDirectory("status-check").toFile();

            Git git = Git.cloneRepository()
                    .setURI(repo.getRepoUrl())
                    .setBranch(app.getBranch())
                    .setDirectory(tempDir)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(repo.getUsername(), repo.getToken()))
                    .call();

            String localHead = git.getRepository().resolve("HEAD").getName();
            String remoteHead = Git.lsRemoteRepository()
                    .setRemote(repo.getRepoUrl())
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(repo.getUsername(), repo.getToken()))
                    .call()
                    .stream()
                    .filter(ref -> ref.getName().endsWith("/" + app.getBranch()))
                    .findFirst()
                    .map(ref -> ref.getObjectId().getName())
                    .orElse(null);

            String status = localHead.equals(remoteHead) ? "Synced" : "Out of Sync";
            app.setStatus(status);
            statusController.sendStatus(app);
            saveToDisk();
            return status;

        } catch (Exception e) {
            app.setStatus("Error");
            statusController.sendStatus(app);
            saveToDisk();
            return "Error";
        }
    }
}