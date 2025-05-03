package ru.backend.rest.validation.dto;

import lombok.*;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResultDto {
    private boolean valid;
    private String output;
    private List<String> errors;

    public ValidationResultDto(boolean valid, String output) {
        this.valid = valid;
        this.output = output;
        this.errors = Collections.emptyList();
    }
}
