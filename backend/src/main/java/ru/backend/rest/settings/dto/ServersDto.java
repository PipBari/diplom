package ru.backend.rest.settings.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServersDto {
    private String name;
    private String host;
    private String specify_username;
    private int port;
    private String password;
    private String RAM;
    private String CPU;
    private String status = "Unknown";
}
