package ru.backend.rest.git.dto;

import lombok.Data;

@Data
public class GitBranchDeleteRequest {
    private String url;
    private String username;
    private String token;
    private String branch;

    public GitConnectionRequestDto toConnectionDto() {
        GitConnectionRequestDto dto = new GitConnectionRequestDto();
        dto.setRepoUrl(url);
        dto.setUsername(username);
        dto.setToken(token);
        return dto;
    }
}
