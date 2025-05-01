package ru.backend.service.application;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;
import ru.backend.rest.application.dto.ApplicationDto;
import ru.backend.rest.git.dto.GitConnectionRequestDto;
import ru.backend.rest.ws.WebSocketStatusController;
import ru.backend.service.git.GitService;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ApplicationService {

    private final GitService gitService;
    private final WebSocketStatusController statusController;

    private final Map<String, ApplicationDto> applicationStorage = new ConcurrentHashMap<>();

    public ApplicationService(GitService gitService, WebSocketStatusController statusController) {
        this.gitService = gitService;
        this.statusController = statusController;
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
    }

    public void delete(String name) {
        applicationStorage.remove(name);
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
            return status;

        } catch (Exception e) {
            app.setStatus("Error");
            statusController.sendStatus(app);
            return "Error";
        }
    }
}