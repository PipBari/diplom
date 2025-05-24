package ru.backend.service.git;

import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import ru.backend.rest.application.dto.ApplicationDto;
import ru.backend.rest.git.dto.CiStatusDto;
import ru.backend.rest.git.dto.GitConnectionRequestDto;
import ru.backend.service.application.ApplicationService;
import ru.backend.service.settings.ServersService;
import ru.backend.util.EncryptionUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class GitflowStatusService {

    private final ApplicationService applicationService;
    private final GitService gitService;

    public CiStatusDto getCiStatus(String appName) {
        ApplicationDto app = applicationService.getAll().stream()
                .filter(a -> a.getName().equals(appName))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Приложение не найдено: " + appName));

        if (app.getRepoName() == null || app.getBranch() == null) {
            throw new IllegalArgumentException("У приложения отсутствуют repoName или branch");
        }

        GitConnectionRequestDto repo = gitService.getByName(app.getRepoName());

        String decryptedToken = EncryptionUtils.decrypt(repo.getToken());

        String repoUrl = repo.getRepoUrl();
        String[] parts = repoUrl.replace("https://github.com/", "").replace(".git", "").split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Некорректный формат URL репозитория: " + repoUrl);
        }
        String owner = parts[0];
        String repoName = parts[1];

        String apiUrl = String.format("https://api.github.com/repos/%s/%s/actions/workflows/deploy.yml/runs?branch=%s&per_page=1", owner, repoName, app.getBranch());

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", "Bearer " + decryptedToken)
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                JSONArray runs = json.getJSONArray("workflow_runs");
                if (runs.length() > 0) {
                    JSONObject latestRun = runs.getJSONObject(0);
                    String status = latestRun.getString("status");
                    String conclusion = latestRun.optString("conclusion", "in_progress");
                    String htmlUrl = latestRun.getString("html_url");

                    return new CiStatusDto(app.getRepoName(), app.getBranch(), htmlUrl, conclusion);
                } else {
                    return new CiStatusDto(app.getRepoName(), app.getBranch(), null, "no_runs");
                }
            } else {
                throw new RuntimeException("Ошибка при запросе к GitHub API: " + response.statusCode() + "\n" + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Ошибка при получении статуса CI: " + e.getMessage(), e);
        }
    }
}
