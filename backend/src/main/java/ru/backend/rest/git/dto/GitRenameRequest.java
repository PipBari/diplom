package ru.backend.rest.git.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor @NoArgsConstructor
public class GitRenameRequest {
    private String oldPath;
    private String newPath;
    private String commitMessage;
}
