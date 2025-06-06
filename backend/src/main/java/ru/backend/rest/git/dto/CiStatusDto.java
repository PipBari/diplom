package ru.backend.rest.git.dto;

public class CiStatusDto {
    private String repoName;
    private String branch;
    private String workflowUrl;
    private String status;

    public CiStatusDto() {
    }

    public CiStatusDto(String repoName, String branch, String workflowUrl, String status) {
        this.repoName = repoName;
        this.branch = branch;
        this.workflowUrl = workflowUrl;
        this.status = status;
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

    public String getWorkflowUrl() {
        return workflowUrl;
    }

    public void setWorkflowUrl(String workflowUrl) {
        this.workflowUrl = workflowUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
