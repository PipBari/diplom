package ru.backend.rest.validation.dto;

import java.util.Collections;
import java.util.List;

public class ValidationResultDto {
    private boolean valid;
    private String output;
    private List<String> errors;

    public ValidationResultDto() {
    }

    public ValidationResultDto(boolean valid, String output, List<String> errors) {
        this.valid = valid;
        this.output = output;
        this.errors = errors;
    }

    public ValidationResultDto(boolean valid, String output) {
        this.valid = valid;
        this.output = output;
        this.errors = Collections.emptyList();
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
