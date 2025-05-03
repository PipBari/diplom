package ru.backend.rest.git.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor @AllArgsConstructor
public class GitFileSaveRequest {
    private String path;
    private String content;
    private String commitMessage;
    private String serverName;
}
