package ru.backend.rest.application.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDto {
    private String name;
    private String repoName;
    private String branch;
    private String path;
    private String projectName;
    private String serverName;
    private String syncStrategy;
    private String createdAt;
    private String status = "Not Synced";
}

