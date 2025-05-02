package ru.backend.rest.git.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor @AllArgsConstructor
public class GitRevertRequest {
    private String commitHash;
    private String commitMessage;
}
