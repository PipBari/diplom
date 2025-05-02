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
        try {
            File tempDir = Files.createTempDirectory("tf-validate").toFile();
            File tfFile = new File(tempDir, "main.tf");
            Files.writeString(tfFile.toPath(), request.getContent());

            log.info("[Terraform] Temp path: {}", tempDir.getAbsolutePath());

            ProcessBuilder init = new ProcessBuilder(
                    "docker", "run", "--rm",
                    "-v", tempDir.getAbsolutePath() + ":/data",
                    "-e", "TF_IN_AUTOMATION=1",
                    "hashicorp/terraform:light",
                    "init", "-backend=false"
            );
            init.directory(tempDir);
            Process initProcess = init.start();
            initProcess.waitFor(15, TimeUnit.SECONDS);

            ProcessBuilder validate = new ProcessBuilder(
                    "docker", "run", "--rm",
                    "-v", tempDir.getAbsolutePath() + ":/data",
                    "-e", "TF_IN_AUTOMATION=1",
                    "hashicorp/terraform:light",
                    "validate"
            );
            validate.directory(tempDir);
            Process process = validate.start();

            boolean finished = process.waitFor(20, TimeUnit.SECONDS);
            String stderr = new String(process.getErrorStream().readAllBytes());
            String stdout = new String(process.getInputStream().readAllBytes());

            log.info("[Terraform] Exit code: {}", process.exitValue());
            log.debug("[Terraform] STDOUT:\n{}", stdout);
            log.debug("[Terraform] STDERR:\n{}", stderr);

            if (!finished) {
                return new ValidationResultDto(false, "Таймаут terraform validate");
            }

            if (process.exitValue() != 0) {
                if (stderr.contains("Missing required provider")) {
                    return new ValidationResultDto(true, "Провайдеры отсутствуют, требуется `terraform init`.");
                }
                return new ValidationResultDto(false, "Ошибка Terraform:\n" + stderr + stdout);
            }

            return new ValidationResultDto(true, "Terraform: OK");

        } catch (Exception e) {
            log.error("Terraform validation failed", e);
            return new ValidationResultDto(false, "Terraform ошибка: " + e.getMessage());
        }
    }

    public ValidationResultDto validateAnsible(ValidationRequestDto request) {
        try {
            try (InputStream is = new ByteArrayInputStream(request.getContent().getBytes())) {
                new ObjectMapper(new YAMLFactory()).readTree(is);
            } catch (Exception yamlError) {
                return new ValidationResultDto(false, "YAML ошибка: " + yamlError.getMessage());
            }

            File tempDir = Files.createTempDirectory("ansible-validate").toFile();
            File ymlFile = new File(tempDir, "playbook.yml");
            Files.writeString(ymlFile.toPath(), request.getContent());

            log.info("[Ansible] Temp path: {}", tempDir.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "run", "--rm",
                    "-v", tempDir.getAbsolutePath() + ":/data",
                    "cytopia/ansible-lint",
                    "/data/playbook.yml"
            );

            Process process = pb.start();
            boolean finished = process.waitFor(20, TimeUnit.SECONDS);
            String stderr = new String(process.getErrorStream().readAllBytes());
            String stdout = new String(process.getInputStream().readAllBytes());

            log.info("[Ansible] Exit code: {}", process.exitValue());
            log.debug("[Ansible] STDOUT:\n{}", stdout);
            log.debug("[Ansible] STDERR:\n{}", stderr);

            if (!finished) {
                return new ValidationResultDto(false, "Таймаут ansible-lint");
            }

            if (process.exitValue() != 0) {
                return new ValidationResultDto(false, "Ansible-lint:\n" + stdout + stderr);
            }

            return new ValidationResultDto(true, "Ansible-lint: OK");

        } catch (Exception e) {
            log.error("Ansible validation failed", e);
            return new ValidationResultDto(false, "Ansible-lint ошибка: " + e.getMessage());
        }
    }
}
