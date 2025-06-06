package ru.backend.rest.git.dto;

public class GitCommitDto {
    private String author;
    private String message;
    private String date;
    private String hash;

    public GitCommitDto() {}

    public GitCommitDto(String author, String message, String date, String hash) {
        this.author = author;
        this.message = message;
        this.date = date;
        this.hash = hash;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
}
