package com.merdeleine.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.merdeleine.notification.dto.AdminBroadcastRequest;
import com.merdeleine.notification.dto.AdminPushRequest;
import com.merdeleine.notification.repository.LineUserRepository;
import com.merdeleine.notification.service.LineMessagingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/line")
@Tag(name = "Admin LINE", description = "Admin endpoints for LINE messaging")
public class AdminLineMessageController {

    private final LineMessagingService lineMessagingService;
    private final LineUserRepository lineUserRepository;

    public AdminLineMessageController(LineMessagingService lineMessagingService,
                                      LineUserRepository lineUserRepository) {
        this.lineMessagingService = lineMessagingService;
        this.lineUserRepository = lineUserRepository;
    }

    @PostMapping("/broadcast")
    @Operation(summary = "Broadcast message to all LINE users")
    public ResponseEntity<Void> broadcast(@Valid @RequestBody AdminBroadcastRequest request) {
        lineMessagingService.broadcastMessage(request.getMessage());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/push")
    @Operation(summary = "Push message to one specified LINE user")
    public ResponseEntity<Void> pushToUser(@Valid @RequestBody AdminPushRequest request) {
        return lineUserRepository.findByUserId(request.getUserId())
                .map(lineUser -> {
                    if (!lineUser.isFollowed()) {
                        return ResponseEntity.status(409).<Void>build();
                    }
                    lineMessagingService.pushMessage(request.getUserId(), request.getMessage());
                    return ResponseEntity.ok().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
