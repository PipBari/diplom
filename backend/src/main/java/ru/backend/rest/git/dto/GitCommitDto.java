package ru.backend.rest.git.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor @AllArgsConstructor
public class GitCommitDto {
    private String author;
    private String message;
    private String date;
}

