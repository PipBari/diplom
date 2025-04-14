package ru.backend.rest.application.dto;

import lombok.*;

@Data
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ApplicationDto {
    private String name;
    private String path;
    private String createdAt;
}
