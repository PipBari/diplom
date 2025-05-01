package ru.backend.rest.git.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileEntryDto {
    private String type;
    private String name;
    private String content;
    private List<FileNodeDto> children;
}

