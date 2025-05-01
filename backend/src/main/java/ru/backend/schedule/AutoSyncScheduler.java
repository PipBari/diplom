package ru.backend.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.backend.rest.application.dto.ApplicationDto;
import ru.backend.service.application.ApplicationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoSyncScheduler {

    private final ApplicationService applicationService;

    @Scheduled(fixedRate = 60000)
    public void syncAutoApps() {
        for (ApplicationDto app : applicationService.getAll()) {
            if ("auto".equalsIgnoreCase(app.getSyncStrategy())) {
                String status = applicationService.recheckStatus(app.getName());
                log.info("Авто-синхронизация [{}]: {}", app.getName(), status);
            }
        }
    }
}
