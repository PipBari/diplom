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
        log.info("Проверка статуса и обновление нагрузки всех серверов");
        serversService.getAll().forEach(server -> {
            String name = server.getName();
            String newStatus = serversService.recheckStatus(name);

            if ("Successful".equals(newStatus)) {
                serversService.updateServerLoad(name);
            } else {
                server.setCPU("-");
                server.setRAM("-");
            }
        });
    }
}
