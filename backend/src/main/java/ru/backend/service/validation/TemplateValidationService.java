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

    public ValidationResultDto validate(String type, ValidationRequestDto request) {
        switch (type.toLowerCase()) {
            case "terraform":
                return validateTerraform(request);
            case "ansible":
                return validateAnsible(request);
            default:
                return new ValidationResultDto(true, "OK (валидация не требуется)");
        }
    }

    public ValidationResultDto validateTerraform(ValidationRequestDto request) {
        if (request.getFilename() == null || request.getFilename().isBlank()) {
            return new ValidationResultDto(false, "Поле filename отсутствует или пустое");
        }

        File tempDir = null;
        try {
            tempDir = Files.createTempDirectory("tf-validate").toFile();

            File file = new File(tempDir, request.getFilename());
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            Files.writeString(file.toPath(), request.getContent(), StandardCharsets.UTF_8);

            ProcessBuilder init = new ProcessBuilder(
                    "docker", "run", "--rm",
                    "-v", tempDir.getAbsolutePath() + ":/data",
                    "-e", "TF_IN_AUTOMATION=1",
                    "hashicorp/terraform:light",
                    "init", "-backend=false"
            );
            Process initProcess = init.start();
            initProcess.waitFor(15, TimeUnit.SECONDS);

            ProcessBuilder validate = new ProcessBuilder(
                    "docker", "run", "--rm",
                    "-v", tempDir.getAbsolutePath() + ":/data",
                    "-e", "TF_IN_AUTOMATION=1",
                    "hashicorp/terraform:light",
                    "validate", "-no-color"
            );
            Process process = validate.start();
            boolean finished = process.waitFor(20, TimeUnit.SECONDS);
            String stderr = new String(process.getErrorStream().readAllBytes());
            String stdout = new String(process.getInputStream().readAllBytes());

            if (!finished) {
                return new ValidationResultDto(false, "Таймаут terraform validate");
            }

            if (process.exitValue() != 0) {
                return new ValidationResultDto(false, "Terraform ошибка", List.of(stderr + stdout));
            }

            if (request.getServerName() != null && !request.getServerName().isBlank()) {
                ServersDto server = serversService.getAll().stream()
                        .filter(s -> s.getName().equals(request.getServerName()))
                        .findFirst()
                        .orElse(null);

                if (server != null && "Successful".equals(server.getStatus())) {
                    int serverRamMb = parseAvailableRam(server.getRAM());
                    double serverCpuFreePercent = parseFreeCpuPercent(server.getCPU());

                    Map<String, Integer> vars = extractVarsFromAllFiles(tempDir, List.of("ram", "cpu"));
                    int requestedRamMb = vars.getOrDefault("ram", 0);
                    int requestedCpu = vars.getOrDefault("cpu", 0);

                    if (requestedRamMb > serverRamMb) {
                        return new ValidationResultDto(false,
                                "Недостаточно оперативной памяти: требуется %d МБ, доступно %d МБ"
                                        .formatted(requestedRamMb, serverRamMb));
                    }

                    if (requestedCpu * 100 > serverCpuFreePercent) {
                        return new ValidationResultDto(false,
                                "Недостаточно CPU: требуется %d vCPU (~%d%%), доступно ~%.1f%%"
                                        .formatted(requestedCpu, requestedCpu * 100, serverCpuFreePercent));
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

    public ValidationResultDto validateAnsible(ValidationRequestDto request) {
        File tempDir = null;
        try {
            try (InputStream is = new ByteArrayInputStream(request.getContent().getBytes())) {
                new ObjectMapper(new YAMLFactory()).readTree(is);
            } catch (Exception yamlError) {
                return new ValidationResultDto(false, "YAML ошибка", List.of(yamlError.getMessage()));
            }

            tempDir = Files.createTempDirectory("ansible-validate").toFile();
            File ymlFile = new File(tempDir, "playbook.yml");
            Files.writeString(ymlFile.toPath(), request.getContent());

            ProcessBuilder syntaxCheck = new ProcessBuilder(
                    "docker", "run", "--rm",
                    "-v", tempDir.getAbsolutePath() + ":/data",
                    "willhallonline/ansible:latest",
                    "ansible-playbook", "--syntax-check", "/data/playbook.yml"
            );
            Process syntaxProcess = syntaxCheck.start();
            boolean syntaxFinished = syntaxProcess.waitFor(20, TimeUnit.SECONDS);
            String syntaxOut = new String(syntaxProcess.getInputStream().readAllBytes());
            String syntaxErr = new String(syntaxProcess.getErrorStream().readAllBytes());

            if (!syntaxFinished || syntaxProcess.exitValue() != 0) {
                return new ValidationResultDto(false, "Ansible syntax-check ошибка", List.of(syntaxOut + syntaxErr));
            }

            ProcessBuilder lint = new ProcessBuilder(
                    "docker", "run", "--rm",
                    "-v", tempDir.getAbsolutePath() + ":/data",
                    "cytopia/ansible-lint",
                    "/data/playbook.yml"
            );
            Process lintProcess = lint.start();
            boolean lintFinished = lintProcess.waitFor(20, TimeUnit.SECONDS);
            String lintOut = new String(lintProcess.getInputStream().readAllBytes());
            String lintErr = new String(lintProcess.getErrorStream().readAllBytes());

            if (!lintFinished) {
                return new ValidationResultDto(false, "Таймаут ansible-lint");
            }

            if (lintProcess.exitValue() != 0) {
                return new ValidationResultDto(false, "Ansible-lint ошибка", List.of(lintOut + lintErr));
            }

            return new ValidationResultDto(true, "Ansible: OK");

        } catch (Exception e) {
            log.error("Ansible validation failed", e);
            return new ValidationResultDto(false, "Ansible ошибка: " + e.getMessage());
        } finally {
            if (tempDir != null) deleteDirectory(tempDir);
        }
    }

    private Map<String, Integer> extractVarsFromAllFiles(File dir, List<String> varNames) {
        Map<String, Integer> declaredVars = new HashMap<>();
        Map<String, Integer> result = new HashMap<>();

        Pattern variablePattern = Pattern.compile("variable\\s+\"(\\w+)\"\\s*\\{[^}]*?default\\s*=\\s*(\\d+)", Pattern.DOTALL);
        Pattern usagePattern = Pattern.compile("var\\.(\\w+)");

        Queue<File> queue = new LinkedList<>();
        queue.add(dir);

        while (!queue.isEmpty()) {
            File current = queue.poll();
            if (current.isDirectory()) {
                File[] files = current.listFiles();
                if (files != null) Collections.addAll(queue, files);
            } else if (current.getName().endsWith(".tf")) {
                try {
                    String content = Files.readString(current.toPath());

                    Matcher varMatcher = variablePattern.matcher(content);
                    while (varMatcher.find()) {
                        String name = varMatcher.group(1);
                        int value = Integer.parseInt(varMatcher.group(2));
                        declaredVars.put(name, value);
                    }

                } catch (IOException e) {
                    log.warn("Ошибка чтения файла: {}", current.getName());
                }
            }
        }

        for (Map.Entry<String, Integer> entry : declaredVars.entrySet()) {
            String name = entry.getKey().toLowerCase();
            int value = entry.getValue();

            if (name.contains("ram") || name.contains("memory")) {
                result.put("ram", value);
            } else if (name.contains("cpu") || name.contains("core")) {
                result.put("cpu", value);
            }
        }

        return result;
    }

    private int parseAvailableRam(String ramInfo) {
        try {
            String[] parts = ramInfo.trim().split("/");
            int used = Integer.parseInt(parts[0].replaceAll("[^0-9]", "").trim());
            int total = Integer.parseInt(parts[1].replaceAll("[^0-9]", "").trim());
            return total - used;
        } catch (Exception e) {
            log.warn("Ошибка парсинга RAM: {}", e.getMessage());
            return 0;
        }
    }

    private double parseFreeCpuPercent(String cpuInfo) {
        try {
            return 100.0 - Double.parseDouble(cpuInfo.replace("%", "").trim());
        } catch (Exception e) {
            log.warn("Ошибка парсинга CPU: {}", e.getMessage());
            return 0;
        }
    }

    private void deleteDirectory(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directory.delete();
    }
}
