package ru.backend.rest.git.dto;

public class GitRevertRequest {
    private String commitHash;
    private String commitMessage;

    public GitRevertRequest() {
    }

    public GitRevertRequest(String commitHash, String commitMessage) {
        this.commitHash = commitHash;
        this.commitMessage = commitMessage;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }
}
