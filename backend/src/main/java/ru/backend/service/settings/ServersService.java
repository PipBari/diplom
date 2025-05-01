package ru.backend.service.settings;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.backend.rest.settings.dto.ServersDto;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Scanner;

@Slf4j
@Service
public class ServersService {

    private final Map<String, ServersDto> serverStorage = new ConcurrentHashMap<>();

    public List<ServersDto> getAll() {
        return new ArrayList<>(serverStorage.values());
    }

    public void save(ServersDto server) {
        server.setStatus(checkConnection(server));
        serverStorage.put(server.getName(), server);
        updateServerLoad(server.getName());
    }

    public void delete(String name) {
        serverStorage.remove(name);
    }

    public String recheckStatus(String name) {
        ServersDto server = serverStorage.get(name);
        if (server == null) return "Unknown";

        String status = checkConnection(server);
        server.setStatus(status);
        return status;
    }

    public void updateServerLoad(String name) {
        ServersDto server = serverStorage.get(name);
        if (server == null || !"Successful".equals(server.getStatus())) return;

        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(server.getSpecify_username(), server.getHost(), server.getPort());
            session.setPassword(server.getPassword());

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect(10000);

            String cpuLoad = executeCommand(session, "top -bn1 | grep \"%Cpu\" | awk '{print $2+$4}'");
            server.setCPU(cpuLoad.trim() + "%");

            String memoryInfo = executeCommand(session, "free -m | grep Mem");
            server.setRAM(parseMemory(memoryInfo));

            session.disconnect();
        } catch (Exception e) {
            server.setCPU("-");
            server.setRAM("-");
            log.error("Ошибка при обновлении нагрузки сервера '{}': {}", server.getName(), e.getMessage());
        }
    }

    private String checkConnection(ServersDto server) {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(server.getSpecify_username(), server.getHost(), server.getPort());
            session.setPassword(server.getPassword());

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect(10000);
            session.disconnect();

            return "Successful";
        } catch (Exception e) {
            return "Error";
        }
    }

    private String executeCommand(Session session, String command) throws Exception {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        channel.setInputStream(null);

        InputStream in = channel.getInputStream();
        channel.connect();

        String result;
        try (Scanner scanner = new Scanner(in).useDelimiter("\\A")) {
            result = scanner.hasNext() ? scanner.next() : "";
        }

        channel.disconnect();
        return result;
    }

    private String parseMemory(String memoryInfo) {
        String[] parts = memoryInfo.trim().split("\\s+");
        if (parts.length >= 3) {
            int total = Integer.parseInt(parts[1]);
            int used = Integer.parseInt(parts[2]);
            return used + " МБ / " + total + " МБ";
        }
        return "Unknown";
    }
}
