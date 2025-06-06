package ru.backend.rest.git.dto;

public class GitBranchDeleteRequest {
    private String url;
    private String username;
    private String token;
    private String branch;

    public GitBranchDeleteRequest() {}

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

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public GitConnectionRequestDto toConnectionDto() {
        GitConnectionRequestDto dto = new GitConnectionRequestDto();
        dto.setRepoUrl(url);
        dto.setUsername(username);
        dto.setToken(token);
        return dto;
    }
}
