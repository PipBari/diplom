package ru.backend.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.backend.service.settings.ServersService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServerMonitoringScheduler {

    private final ServersService serversService;

    @Scheduled(fixedRate = 60000)
    public void updateServerMetrics() {
        log.info("Обновление нагрузки всех серверов");
        serversService.getAll().forEach(server ->
                serversService.updateServerLoad(server.getName())
        );
    }
}
