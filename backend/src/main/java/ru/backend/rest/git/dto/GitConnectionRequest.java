package ru.backend.rest.git.dto;

import lombok.*;

@Data
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class GitConnectionRequest {
    private String repoUrl;
    private String localPath;
    private String branch;
    private String username;
    private String token;
}
