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

    private final GitflowGeneratorService gitflowGeneratorService;

    @PostMapping
    public ResponseEntity<String> generateGitflow(@PathVariable String name) {
        try {
            gitflowGeneratorService.generateGitflow(name);
            return ResponseEntity.ok("Gitflow успешно сгенерирован и отправлен в репозиторий");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException | GitAPIException e) {
            return ResponseEntity.status(500).body("Ошибка при генерации gitflow: " + e.getMessage());
        }
    }
}
