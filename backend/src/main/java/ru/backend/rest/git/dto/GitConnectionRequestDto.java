package ru.backend.rest.git.dto;

public class GitConnectionRequestDto {
    private String name;
    private String repoUrl;
    private String branch;
    private String username;
    private String token;
    private String type = "git";
    private String status = "Unknown";

    public GitConnectionRequestDto() {}

    public GitConnectionRequestDto(String name, String repoUrl, String branch,
                                   String username, String token, String type, String status) {
        this.name = name;
        this.repoUrl = repoUrl;
        this.branch = branch;
        this.username = username;
        this.token = token;
        this.type = type;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
