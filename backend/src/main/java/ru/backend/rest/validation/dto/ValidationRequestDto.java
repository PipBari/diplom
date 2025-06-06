package ru.backend.rest.validation.dto;

public class ValidationRequestDto {
    private String filename;
    private String content;
    private String serverName;

    public ValidationRequestDto() {
    }

    public ValidationRequestDto(String filename, String content, String serverName) {
        this.filename = filename;
        this.content = content;
        this.serverName = serverName;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
}
