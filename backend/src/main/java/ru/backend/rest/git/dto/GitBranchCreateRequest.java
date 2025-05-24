package ru.backend.rest.git.dto;

import lombok.Data;

@Data
public class GitBranchCreateRequest {
    private String url;
    private String username;
    private String token;
    private String name;
    private String from;

    public GitConnectionRequestDto toConnectionDto() {
        GitConnectionRequestDto dto = new GitConnectionRequestDto();
        dto.setRepoUrl(url);
        dto.setUsername(username);
        dto.setToken(token);
        return dto;
    }
}

