package ru.backend.rest.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.backend.rest.application.dto.ApplicationDto;

@Component
@RequiredArgsConstructor
public class WebSocketStatusController {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendStatus(ApplicationDto app) {
        messagingTemplate.convertAndSend("/topic/status", app);
    }
}
