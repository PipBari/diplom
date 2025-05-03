package ru.backend.service.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.backend.rest.validation.dto.ValidationRequestDto;
import ru.backend.rest.validation.dto.ValidationResultDto;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TemplateValidationService {

    private static final Logger log = LoggerFactory.getLogger(TemplateValidationService.class);

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
        File tempDir = null;
        try {
            tempDir = Files.createTempDirectory("tf-validate").toFile();
            File tfFile = new File(tempDir, "main.tf");
            Files.writeString(tfFile.toPath(), request.getContent());

            ProcessBuilder fmt = new ProcessBuilder(
                    "docker", "run", "--rm",
                    "-v", tempDir.getAbsolutePath() + ":/data",
                    "hashicorp/terraform:light",
                    "fmt", "-check", "-no-color", "/data/main.tf"
            );
            Process fmtProcess = fmt.start();
            fmtProcess.waitFor(10, TimeUnit.SECONDS);
            String fmtOutput = new String(fmtProcess.getInputStream().readAllBytes());
            String fmtError = new String(fmtProcess.getErrorStream().readAllBytes());

            if (fmtProcess.exitValue() != 0) {
                return new ValidationResultDto(false, "Terraform fmt: форматирование не соответствует стилю", List.of(fmtOutput + fmtError));
            }

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
                return new ValidationResultDto(false, "Таймаут terraform validate", List.of());
            }

            if (process.exitValue() != 0) {
                if (stderr.contains("Missing required provider")) {
                    return new ValidationResultDto(true, "Провайдеры отсутствуют, требуется `terraform init`.", List.of(stderr + stdout));
                }
                return new ValidationResultDto(false, "Terraform ошибка", List.of(stderr + stdout));
            }

            return new ValidationResultDto(true, "Terraform: OK");

        } catch (Exception e) {
            log.error("Terraform validation failed", e);
            return new ValidationResultDto(false, "Terraform ошибка: " + e.getMessage(), List.of());
        } finally {
            if (tempDir != null) {
                deleteDirectory(tempDir);
            }
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
                return new ValidationResultDto(false, "Таймаут ansible-lint", List.of());
            }

            if (lintProcess.exitValue() != 0) {
                return new ValidationResultDto(false, "Ansible-lint ошибка", List.of(lintOut + lintErr));
            }

            return new ValidationResultDto(true, "Ansible: OK");

        } catch (Exception e) {
            log.error("Ansible validation failed", e);
            return new ValidationResultDto(false, "Ansible ошибка: " + e.getMessage(), List.of());
        } finally {
            if (tempDir != null) {
                deleteDirectory(tempDir);
            }
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
