package ru.backend.service.settings;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.backend.rest.settings.dto.ServersDto;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Scanner;

@Slf4j
@Service
public class ServersService {

    private final Map<String, ServersDto> serverStorage = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final File storageFile;

    public ServersService(@Value("${storage.base-dir}") String baseDirPath) {
        File baseDir = new File(resolvePath(baseDirPath));
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        this.storageFile = new File(baseDir, "servers.json");
    }

    private String resolvePath(String path) {
        return path.replace("${user.home}", System.getProperty("user.home"));
    }

    @PostConstruct
    public void loadFromDisk() {
        try {
            if (storageFile.exists()) {
                List<ServersDto> list = objectMapper.readValue(storageFile, new TypeReference<>() {});
                for (ServersDto dto : list) {
                    serverStorage.put(dto.getName(), dto);
                }
                log.info("Загружено {} серверов из файла", serverStorage.size());
            }
        } catch (Exception e) {
            log.error("Ошибка при загрузке серверов: ", e);
        }
    }

    @PreDestroy
    public void saveToDisk() {
        try {
            List<ServersDto> list = new ArrayList<>(serverStorage.values());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storageFile, list);
            log.info("Сохранено {} серверов в файл", list.size());
        } catch (Exception e) {
            log.error("Ошибка при сохранении серверов: ", e);
        }
    }

    public List<ServersDto> getAll() {
        return new ArrayList<>(serverStorage.values());
    }

    public void save(ServersDto server) {
        server.setStatus(checkConnection(server));
        serverStorage.put(server.getName(), server);
        updateServerLoad(server.getName());
        saveToDisk();
    }

    public void delete(String name) {
        serverStorage.remove(name);
        saveToDisk();
    }

    public void update(String name, ServersDto updated) {
        ServersDto existing = serverStorage.get(name);
        if (existing == null) {
            throw new NoSuchElementException("Сервер не найден: " + name);
        }

        if (updated.getHost() != null) existing.setHost(updated.getHost());
        if (updated.getSpecify_username() != null) existing.setSpecify_username(updated.getSpecify_username());
        if (updated.getPort() != 0) existing.setPort(updated.getPort());
        if (updated.getPassword() != null) existing.setPassword(updated.getPassword());

        existing.setStatus(checkConnection(existing));
        updateServerLoad(existing.getName());

        serverStorage.put(name, existing);
        saveToDisk();
    }

    public String recheckStatus(String name) {
        ServersDto server = serverStorage.get(name);
        if (server == null) return "Unknown";

        String status = checkConnection(server);
        server.setStatus(status);
        saveToDisk();
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

            saveToDisk();
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
