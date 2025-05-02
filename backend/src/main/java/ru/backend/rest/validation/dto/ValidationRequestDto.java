package ru.backend.rest.validation.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRequestDto {
    private String filename;
    private String content;
}
