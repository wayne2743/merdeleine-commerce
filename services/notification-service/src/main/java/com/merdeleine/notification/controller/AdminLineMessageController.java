package com.merdeleine.notification.controller;

import com.merdeleine.notification.dto.AdminBroadcastRequest;
import com.merdeleine.notification.service.LineMessagingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/line")
public class AdminLineMessageController {

    private final LineMessagingService lineMessagingService;

    public AdminLineMessageController(LineMessagingService lineMessagingService) {
        this.lineMessagingService = lineMessagingService;
    }

    @PostMapping("/broadcast")
    public ResponseEntity<Void> broadcast(@Valid @RequestBody AdminBroadcastRequest request) {
        lineMessagingService.broadcastMessage(request.getMessage());
        return ResponseEntity.ok().build();
    }
}
