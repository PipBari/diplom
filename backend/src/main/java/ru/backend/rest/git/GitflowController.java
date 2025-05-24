package ru.backend.rest.git;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.backend.service.git.GitflowGeneratorService;

import java.io.IOException;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/applications/{name}/gitflow")
@RequiredArgsConstructor
public class GitflowController {

    private final GitflowGeneratorService gitflowGeneratorService;

    @PostMapping
    public ResponseEntity<?> generateGitflow(@PathVariable String name) {
        try {
            gitflowGeneratorService.generateGitflow(name);
            return ResponseEntity.status(201).body("Gitflow успешно сгенерирован и отправлен в репозиторий");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("Приложение не найдено: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Ошибка в параметрах: " + e.getMessage());
        } catch (IOException | GitAPIException e) {
            return ResponseEntity.status(500).body("Ошибка при генерации Gitflow: " + e.getMessage());
        }
    }
}
