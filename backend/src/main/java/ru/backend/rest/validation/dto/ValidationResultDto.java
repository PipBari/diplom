package ru.backend.rest.validation.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResultDto {
    private boolean valid;
    private String output;
}
