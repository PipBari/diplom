package ru.backend.rest.git.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor @AllArgsConstructor
public class FileNodeDto {
    private String name;
    private String type;
    private String content;
    private List<FileNodeDto> children = new ArrayList<>();
}

