package ru.backend.rest.git.dto;

public class GitRenameRequest {
    private String oldPath;
    private String newPath;
    private String commitMessage;

    public GitRenameRequest() {}

    public GitRenameRequest(String oldPath, String newPath, String commitMessage) {
        this.oldPath = oldPath;
        this.newPath = newPath;
        this.commitMessage = commitMessage;
    }

    public String getOldPath() {
        return oldPath;
    }

    public void setOldPath(String oldPath) {
        this.oldPath = oldPath;
    }

    public String getNewPath() {
        return newPath;
    }

    public void setNewPath(String newPath) {
        this.newPath = newPath;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }
}
