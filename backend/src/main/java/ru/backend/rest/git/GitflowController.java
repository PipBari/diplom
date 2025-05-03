package ru.backend.rest.git;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.backend.rest.application.dto.ApplicationDto;
import ru.backend.service.application.ApplicationService;
import ru.backend.service.git.GitService;
import ru.backend.service.git.GitflowGeneratorService;
import ru.backend.service.settings.ServersService;
import ru.backend.rest.git.dto.GitConnectionRequestDto;
import ru.backend.rest.settings.dto.ServersDto;

import java.io.IOException;

@RestController
@RequestMapping("/applications/{name}/gitflow")
@RequiredArgsConstructor
public class GitflowController {

    private final ApplicationService applicationService;
    private final ServersService serversService;
    private final GitService gitService;
    private final GitflowGeneratorService gitflowGeneratorService;

    @PostMapping
    public ResponseEntity<String> generateGitflow(@PathVariable String name) {
        ApplicationDto app = applicationService.getAll().stream()
                .filter(a -> a.getName().equals(name))
                .findFirst()
                .orElse(null);

        if (app == null) {
            return ResponseEntity.badRequest().body("Приложение не найдено");
        }

        if (app.getRepoName() == null || app.getBranch() == null || app.getPath() == null) {
            return ResponseEntity.badRequest().body("Недостаточно данных для генерации gitflow");
        }

        GitConnectionRequestDto repo = gitService.getByName(app.getRepoName());

        ServersDto server = null;
        if (app.getServerName() != null) {
            server = serversService.getAll().stream()
                    .filter(s -> s.getName().equals(app.getServerName()))
                    .findFirst()
                    .orElse(null);
        }

        if (server == null) {
            return ResponseEntity.badRequest().body("Сервер не найден или не указан");
        }

        try {
            gitflowGeneratorService.generate(app, server, repo);
            return ResponseEntity.ok("Gitflow успешно сгенерирован и отправлен в репозиторий");
        } catch (IOException | GitAPIException e) {
            return ResponseEntity.status(500).body("Ошибка при генерации gitflow: " + e.getMessage());
        }
    }
}
