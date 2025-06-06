package ru.backend.rest.git.dto;

import ru.backend.rest.validation.dto.ValidationRequestDto;
import java.util.List;

public class GitFileSaveRequest {
    private String path;
    private String content;
    private String commitMessage;
    private String serverName;
    private List<ValidationRequestDto> allFiles;

    public GitFileSaveRequest() {}

    public GitFileSaveRequest(String path, String content, String commitMessage,
                              String serverName, List<ValidationRequestDto> allFiles) {
        this.path = path;
        this.content = content;
        this.commitMessage = commitMessage;
        this.serverName = serverName;
        this.allFiles = allFiles;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public List<ValidationRequestDto> getAllFiles() {
        return allFiles;
    }

    public void setAllFiles(List<ValidationRequestDto> allFiles) {
        this.allFiles = allFiles;
    }
}
