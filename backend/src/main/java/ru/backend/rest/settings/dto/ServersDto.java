package ru.backend.rest.settings.dto;

public class ServersDto {
    private String name;
    private String host;
    private String specify_username;
    private int port;
    private String password;
    private String RAM;
    private String CPU;
    private String status = "Unknown";

    public ServersDto() {
    }

    public ServersDto(String name, String host, String specify_username, int port,
                      String password, String RAM, String CPU, String status) {
        this.name = name;
        this.host = host;
        this.specify_username = specify_username;
        this.port = port;
        this.password = password;
        this.RAM = RAM;
        this.CPU = CPU;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getSpecify_username() {
        return specify_username;
    }

    public void setSpecify_username(String specify_username) {
        this.specify_username = specify_username;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRAM() {
        return RAM;
    }

    public void setRAM(String RAM) {
        this.RAM = RAM;
    }

    public String getCPU() {
        return CPU;
    }

    public void setCPU(String CPU) {
        this.CPU = CPU;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
