package ru.backend.rest.git.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitConnectionRequest {
    private String name;
    private String repoUrl;
    private String branch;
    private String username;
    private String token;
    private String type = "git";
    private String status = "Unknown";
}