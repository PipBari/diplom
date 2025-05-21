package ru.backend.service.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.backend.rest.settings.dto.ServersDto;
import ru.backend.rest.validation.dto.ValidationRequestDto;
import ru.backend.rest.validation.dto.ValidationResultDto;
import ru.backend.service.settings.ServersService;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TemplateValidationService {

    private static final Logger log = LoggerFactory.getLogger(TemplateValidationService.class);
    private final ServersService serversService;

    public ValidationResultDto validate(String type, List<ValidationRequestDto> requests) {
        switch (type.toLowerCase()) {
            case "terraform":
                return validateTerraform(requests);
            case "ansible":
                return validateAnsible(requests.get(0));
            case "bash":
            case "shell":
                return validateBash(requests.get(0));
            default:
                return new ValidationResultDto(true, "OK (валидация не требуется)");
        }
    }

    public ValidationResultDto validateTerraform(List<ValidationRequestDto> requests) {
        if (requests.isEmpty()) {
            return new ValidationResultDto(false, "Список файлов пуст");
        }

        File tempDir = null;
        try {
            tempDir = Files.createTempDirectory("tf-validate").toFile();

            for (ValidationRequestDto req : requests) {
                File f = new File(tempDir, req.getFilename());
                if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
                Files.writeString(f.toPath(), req.getContent(), StandardCharsets.UTF_8);
            }

            Map.Entry<String, String> syntaxError = findInvalidSyntaxFile(tempDir);
            if (syntaxError != null) {
                return new ValidationResultDto(false,
                        "Terraform синтаксическая ошибка в файле: " + syntaxError.getKey(),
                        List.of(syntaxError.getValue()));
            }

            runDockerTerraform(tempDir, "init", "-backend=false");
            runDockerTerraform(tempDir, "validate", "-no-color");

            ValidationRequestDto current = requests.get(requests.size() - 1);
            List<ValidationRequestDto> others = requests.subList(0, requests.size() - 1);

            String serverName = current.getServerName();
            if (serverName != null && !serverName.isBlank()) {
                ServersDto server = serversService.getAll().stream()
                        .filter(s -> s.getName().equals(serverName))
                        .findFirst().orElse(null);

                if (server != null && "Successful".equals(server.getStatus())) {
                    int totalRam = parseTotalRam(server.getRAM());
                    int usedRam = parseUsedRam(server.getRAM());
                    double availableCpuPercent = parseFreeCpuPercent(server.getCPU());

                    log.info("[RESOURCE] Сервер {}: всего {} МБ, использовано системой {} МБ", serverName, totalRam, usedRam);

                    Map<String, Integer> currentRes = extractVarsFromRequests(List.of(current));
                    Map<String, Integer> othersRes = extractVarsFromRequests(others);

                    int currentRam = currentRes.getOrDefault("ram", 0);
                    int otherRam = othersRes.getOrDefault("ram", 0);
                    int availableRam = totalRam - usedRam - otherRam;

                    int currentCpu = currentRes.getOrDefault("cpu", 0);
                    int otherCpu = othersRes.getOrDefault("cpu", 0);
                    double availableCpu = availableCpuPercent - (otherCpu * 100);

                    log.info("[RESOURCE] Уже выделено .tf-файлами: {} МБ RAM, {} vCPU", otherRam, otherCpu);
                    log.info("[RESOURCE] Текущий запрос: {} МБ RAM, {} vCPU", currentRam, currentCpu);
                    log.info("[RESOURCE] Доступно: {} МБ RAM, ~{}% CPU", availableRam, availableCpu);

                    if (currentRam > availableRam) {
                        return new ValidationResultDto(false,
                                "Недостаточно оперативной памяти: требуется %d МБ, доступно %d МБ (всего %d, занято системой %d, другими файлами %d)"
                                        .formatted(currentRam, availableRam, totalRam, usedRam, otherRam));
                    }

                    if (currentCpu * 100 > availableCpu) {
                        return new ValidationResultDto(false,
                                "Недостаточно CPU: требуется %d vCPU (~%d%%), доступно ~%.1f%% (всего 100%%, уже занято ~%.1f%% другими файлами)"
                                        .formatted(currentCpu, currentCpu * 100, availableCpu, otherCpu * 100.0));
                    }
                }
            }

            return new ValidationResultDto(true, "Terraform: OK");

        } catch (Exception e) {
            log.error("Terraform validation failed", e);
            return new ValidationResultDto(false, "Terraform ошибка: " + e.getMessage());
        } finally {
            if (tempDir != null) deleteDirectory(tempDir);
        }
    }

    private void runDockerTerraform(File tempDir, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("docker", "run", "--rm",
                "-v", tempDir.getAbsolutePath() + ":/data",
                "-e", "TF_IN_AUTOMATION=1",
                "hashicorp/terraform:light"));
        command.addAll(List.of(args));

        ProcessBuilder builder = new ProcessBuilder(command);
        Process process = builder.start();
        boolean finished = process.waitFor(20, TimeUnit.SECONDS);
        String stderr = new String(process.getErrorStream().readAllBytes());
        String stdout = new String(process.getInputStream().readAllBytes());

        log.info("[TERRAFORM {}] STDOUT:\n{}", args[0], stdout);
        log.info("[TERRAFORM {}] STDERR:\n{}", args[0], stderr);

        if (!finished) throw new RuntimeException("Таймаут terraform " + args[0]);
        if (process.exitValue() != 0) throw new RuntimeException("Terraform " + args[0] + " ошибка: " + stderr + stdout);
    }

    private Map<String, Integer> extractVarsFromRequests(List<ValidationRequestDto> requests) {
        Map<String, Integer> totals = new HashMap<>();

        Pattern varPattern = Pattern.compile(
                "variable\\s+\"(\\w+)\"\\s*\\{[^}]*?default\\s*=\\s*(\\d+)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        Pattern localPattern = Pattern.compile(
                "locals\\s*\\{[^}]*?(\\w+)\\s*=\\s*(\\d+)[^}]*?\\}", Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        Pattern resourcePattern = Pattern.compile(
                "(ram|memory|mem_limit|mem|cpu|vcpus|core)\\s*=\\s*(\\d+)", Pattern.CASE_INSENSITIVE
        );

        for (ValidationRequestDto request : requests) {
            try {
                String content = request.getContent();
                log.info("[PARSE] Файл: {}", request.getFilename());

                Matcher varMatcher = varPattern.matcher(content);
                while (varMatcher.find()) {
                    String name = varMatcher.group(1).toLowerCase();
                    int value = Integer.parseInt(varMatcher.group(2));
                    log.info("[VAR] {} = {}", name, value);

                    if (name.contains("ram") || name.contains("mem")) {
                        totals.merge("ram", value, Integer::sum);
                    } else if (name.contains("cpu") || name.contains("core") || name.contains("vcpus")) {
                        totals.merge("cpu", value, Integer::sum);
                    }
                }

                Matcher localMatcher = localPattern.matcher(content);
                while (localMatcher.find()) {
                    String name = localMatcher.group(1).toLowerCase();
                    int value = Integer.parseInt(localMatcher.group(2));
                    log.info("[LOCAL] {} = {}", name, value);

                    if (name.contains("ram") || name.contains("mem")) {
                        totals.merge("ram", value, Integer::sum);
                    } else if (name.contains("cpu") || name.contains("core") || name.contains("vcpus")) {
                        totals.merge("cpu", value, Integer::sum);
                    }
                }

                Matcher resMatcher = resourcePattern.matcher(content);
                while (resMatcher.find()) {
                    String name = resMatcher.group(1).toLowerCase();
                    int value = Integer.parseInt(resMatcher.group(2));
                    log.info("[RESOURCE] {} = {}", name, value);

                    if (name.contains("ram") || name.contains("mem")) {
                        totals.merge("ram", value, Integer::sum);
                    } else if (name.contains("cpu") || name.contains("core") || name.contains("vcpus")) {
                        totals.merge("cpu", value, Integer::sum);
                    }
                }

            } catch (Exception e) {
                log.warn("Ошибка парсинга файла {}: {}", request.getFilename(), e.getMessage());
            }
        }

        log.info("[TF VAR TOTALS] RAM: {}, CPU: {}", totals.getOrDefault("ram", 0), totals.getOrDefault("cpu", 0));
        return totals;
    }

    private Map.Entry<String, String> findInvalidSyntaxFile(File dir) {
        Queue<File> queue = new LinkedList<>();
        queue.add(dir);
        while (!queue.isEmpty()) {
            File current = queue.poll();
            if (current.isDirectory()) {
                File[] children = current.listFiles();
                if (children != null) Collections.addAll(queue, children);
            } else if (current.getName().endsWith(".tf")) {
                try {
                    ProcessBuilder fmtCheck = new ProcessBuilder(
                            "docker", "run", "--rm",
                            "-v", current.getParentFile().getAbsolutePath() + ":/data",
                            "hashicorp/terraform:light",
                            "fmt", "-check", "/data/" + current.getName()
                    );
                    Process process = fmtCheck.start();
                    boolean finished = process.waitFor(5, TimeUnit.SECONDS);
                    String stderr = new String(process.getErrorStream().readAllBytes());
                    String stdout = new String(process.getInputStream().readAllBytes());
                    if (!finished || process.exitValue() != 0) {
                        return Map.entry(current.getName(), stderr + stdout);
                    }
                } catch (Exception e) {
                    return Map.entry(current.getName(), e.getMessage());
                }
            }
        }
        return null;
    }

    private int parseUsedRam(String ramInfo) {
        try {
            return Integer.parseInt(ramInfo.split("/")[0].replaceAll("[^0-9]", "").trim());
        } catch (Exception e) {
            log.warn("Ошибка RAM used: {}", e.getMessage());
            return 0;
        }
    }

    private int parseTotalRam(String ramInfo) {
        try {
            return Integer.parseInt(ramInfo.split("/")[1].replaceAll("[^0-9]", "").trim());
        } catch (Exception e) {
            log.warn("Ошибка RAM total: {}", e.getMessage());
            return 0;
        }
    }

    private double parseFreeCpuPercent(String cpuInfo) {
        try {
            return 100.0 - Double.parseDouble(cpuInfo.replace("%", "").trim());
        } catch (Exception e) {
            log.warn("Ошибка CPU: {}", e.getMessage());
            return 0;
        }
    }

    private void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) deleteDirectory(f);
        dir.delete();
    }

    public ValidationResultDto validateAnsible(ValidationRequestDto request) {
        File tempDir = null;
        try {
            new ObjectMapper(new YAMLFactory()).readTree(new ByteArrayInputStream(request.getContent().getBytes()));

            tempDir = Files.createTempDirectory("ansible-validate").toFile();

            File ymlFile = new File(tempDir, "playbook.yml");
            Files.writeString(ymlFile.toPath(), request.getContent());

            ProcessBuilder syntaxCheck = new ProcessBuilder(
                    "docker", "run", "--rm",
                    "-v", tempDir.getAbsolutePath() + ":/data",
                    "willhallonline/ansible:latest",
                    "ansible-playbook", "--syntax-check", "/data/playbook.yml"
            );
            Process syntax = syntaxCheck.start();
            if (!syntax.waitFor(20, TimeUnit.SECONDS) || syntax.exitValue() != 0) {
                return new ValidationResultDto(false, "Ansible syntax ошибка", List.of(
                        new String(syntax.getInputStream().readAllBytes()) +
                                new String(syntax.getErrorStream().readAllBytes())));
            }

            File configDir = new File(tempDir, ".config");
            if (!configDir.mkdirs() && !configDir.exists()) {
                throw new IOException("Не удалось создать .config для ansible-lint");
            }

            File configFile = new File(configDir, "ansible-lint.yml");
            Files.writeString(configFile.toPath(), """
                skip_list:
                  - yaml[empty-lines]
                  - yaml[line-length]
                """
            );

            ProcessBuilder lint = new ProcessBuilder(
                    "docker", "run", "--rm",
                    "-v", tempDir.getAbsolutePath() + ":/data",
                    "cytopia/ansible-lint",
                    "-c", "/data/.config/ansible-lint.yml",
                    "/data/playbook.yml"
            );

            Process linter = lint.start();
            if (!linter.waitFor(20, TimeUnit.SECONDS)) {
                return new ValidationResultDto(false, "Таймаут ansible-lint");
            }

            if (linter.exitValue() != 0) {
                return new ValidationResultDto(false, "Ansible-lint ошибка", List.of(
                        new String(linter.getInputStream().readAllBytes()) +
                                new String(linter.getErrorStream().readAllBytes())));
            }

            return new ValidationResultDto(true, "Ansible: OK");

        } catch (Exception e) {
            log.error("Ansible validation failed", e);
            return new ValidationResultDto(false, "Ansible ошибка: " + e.getMessage());
        } finally {
            if (tempDir != null) deleteDirectory(tempDir);
        }
    }

    public ValidationResultDto validateBash(ValidationRequestDto request) {
        File tempDir = null;
        try {
            tempDir = Files.createTempDirectory("bash-validate").toFile();
            File scriptFile = new File(tempDir, "script.sh");

            String cleaned = request.getContent().replaceAll("\\r\\n?", "\n");
            if (!cleaned.endsWith("\n")) cleaned += "\n";
            Files.writeString(scriptFile.toPath(), cleaned, StandardCharsets.UTF_8);

            ProcessBuilder builder = new ProcessBuilder(
                    "docker", "run", "--rm",
                    "-v", tempDir.getAbsolutePath() + ":/data",
                    "koalaman/shellcheck",
                    "/data/script.sh"
            );

            Process process = builder.start();
            boolean finished = process.waitFor(15, TimeUnit.SECONDS);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            log.info("[SHELLCHECK STDOUT]:\n{}", stdout);
            log.info("[SHELLCHECK STDERR]:\n{}", stderr);

            if (!finished) {
                return new ValidationResultDto(false, "Таймаут проверки bash-скрипта");
            }

            if (process.exitValue() != 0) {
                return new ValidationResultDto(false, "Bash синтаксическая ошибка", List.of(stdout + stderr));
            }

            return new ValidationResultDto(true, "Bash: OK");

        } catch (Exception e) {
            log.error("Shellcheck validation failed", e);
            return new ValidationResultDto(false, "Bash ошибка: " + e.getMessage());
        } finally {
            if (tempDir != null) deleteDirectory(tempDir);
        }
    }
}
