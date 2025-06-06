package ru.backend.rest.git.dto;

public class GitCreateFolderRequest {
    private String path;
    private String commitMessage;

    public GitCreateFolderRequest() {}

    public GitCreateFolderRequest(String path, String commitMessage) {
        this.path = path;
        this.commitMessage = commitMessage;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }
}
