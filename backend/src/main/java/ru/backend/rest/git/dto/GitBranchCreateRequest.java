package ru.backend.rest.git.dto;

public class GitBranchCreateRequest {
    private String url;
    private String username;
    private String token;
    private String name;
    private String from;

    public GitBranchCreateRequest() {}

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public GitConnectionRequestDto toConnectionDto() {
        GitConnectionRequestDto dto = new GitConnectionRequestDto();
        dto.setRepoUrl(url);
        dto.setUsername(username);
        dto.setToken(token);
        return dto;
    }
}
