package ru.backend.rest.application.dto;

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

    public ApplicationDto() {
    }

    public ApplicationDto(String name, String repoName, String branch, String path,
                          String projectName, String serverName, String syncStrategy,
                          String createdAt, String status) {
        this.name = name;
        this.repoName = repoName;
        this.branch = branch;
        this.path = path;
        this.projectName = projectName;
        this.serverName = serverName;
        this.syncStrategy = syncStrategy;
        this.createdAt = createdAt;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getSyncStrategy() {
        return syncStrategy;
    }

    public void setSyncStrategy(String syncStrategy) {
        this.syncStrategy = syncStrategy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
