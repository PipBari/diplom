package ru.backend.service.git;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;
import ru.backend.rest.git.dto.GitConnectionRequestDto;

import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class GitService {

    private final Map<String, GitConnectionRequestDto> repoStorage = new ConcurrentHashMap<>();

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
    }

    public void delete(String name) {
        repoStorage.remove(name);
    }

    public String recheckStatus(String name) {
        GitConnectionRequestDto repo = repoStorage.get(name);
        if (repo == null) {
            return "Unknown";
        }
        String status = checkConnection(repo);
        repo.setStatus(status);
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
}