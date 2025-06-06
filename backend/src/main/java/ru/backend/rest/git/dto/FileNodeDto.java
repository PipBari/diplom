package ru.backend.rest.git.dto;

import java.util.ArrayList;
import java.util.List;

public class FileNodeDto {
    private String name;
    private String type;
    private String content;
    private List<FileNodeDto> children = new ArrayList<>();

    public FileNodeDto() {
    }

    public FileNodeDto(String name, String type, String content, List<FileNodeDto> children) {
        this.name = name;
        this.type = type;
        this.content = content;
        this.children = children != null ? children : new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<FileNodeDto> getChildren() {
        return children;
    }

    public void setChildren(List<FileNodeDto> children) {
        this.children = children != null ? children : new ArrayList<>();
    }
}
