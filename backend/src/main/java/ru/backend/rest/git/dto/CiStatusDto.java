package ru.backend.rest.git.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CiStatusDto {
    private String repoName;
    private String branch;
    private String workflowUrl;
    private String status;
}
