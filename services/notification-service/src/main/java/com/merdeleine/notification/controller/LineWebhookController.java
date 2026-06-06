package com.merdeleine.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.notification.config.LineMessagingProperties;
import com.merdeleine.notification.dto.line.LineWebhookPayload;
import com.merdeleine.notification.entity.LineUser;
import com.merdeleine.notification.repository.LineUserRepository;
import com.merdeleine.notification.service.LineMessagingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@RestController
@Tag(name = "LINE Webhook", description = "LINE callback endpoint")
public class LineWebhookController {

    private static final Logger log = LoggerFactory.getLogger(LineWebhookController.class);

    private final LineMessagingProperties props;
    private final LineUserRepository lineUserRepository;
    private final LineMessagingService lineMessagingService;
    private final ObjectMapper objectMapper;

    public LineWebhookController(LineMessagingProperties props,
                                 LineUserRepository lineUserRepository,
                                 LineMessagingService lineMessagingService,
                                 ObjectMapper objectMapper) {
        this.props = props;
        this.lineUserRepository = lineUserRepository;
        this.lineMessagingService = lineMessagingService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("${line.messaging.webhook-path:/api/line/webhook}")
    @Transactional
    @Operation(summary = "Receive LINE webhook callback")
    public ResponseEntity<Void> webhook(
            @RequestHeader(value = "X-Line-Signature", required = false) String signature,
            @RequestBody(required = false) String rawBody
    ) {
        if (!StringUtils.hasText(props.getChannelSecret())) {
            log.error("[LINE webhook] LINE_CHANNEL_SECRET is missing; cannot verify webhook signature");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        if (!StringUtils.hasText(signature)) {
            log.warn("[LINE webhook] missing X-Line-Signature header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        rawBody = rawBody == null ? "" : rawBody;

        if (!verifySignature(signature, rawBody)) {
            log.warn("[LINE webhook] invalid signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        LineWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, LineWebhookPayload.class);
        } catch (Exception e) {
            log.error("[LINE webhook] failed to parse payload", e);
            return ResponseEntity.badRequest().build();
        }

        if (payload.events() == null) {
            return ResponseEntity.ok().build();
        }

        for (LineWebhookPayload.LineEvent event : payload.events()) {
            try {                handleEvent(event);

            } catch (Exception e) {
                log.error("[LINE webhook] error handling event type={}", event.type(), e);
            }
        }
        return ResponseEntity.ok().build();
    }

    private void handleEvent(LineWebhookPayload.LineEvent event) {
        if (event.source() == null || event.source().userId() == null) return;
        String userId = event.source().userId();

        switch (event.type()) {
            case "follow" -> {
                log.info("[LINE follow] userId={}", userId);
                lineUserRepository.findByUserId(userId)
                        .ifPresentOrElse(
                                u -> { u.setFollowed(true); lineUserRepository.save(u); },
                                () -> { LineUser u = new LineUser(); u.setUserId(userId); lineUserRepository.save(u); }
                        );
            }
            case "unfollow" -> {
                log.info("[LINE unfollow] userId={}", userId);
                lineUserRepository.findByUserId(userId).ifPresent(u -> {
                    u.setFollowed(false);
                    lineUserRepository.save(u);
                });
            }
            case "message" -> {
                log.info("[LINE message] userId={}", userId);
                if (event.replyToken() != null) {
                    lineMessagingService.replyMessage(event.replyToken(), "已收到訊息，感謝您的聯繫！");
                }
            }
            default -> log.debug("[LINE webhook] unhandled event type={}", event.type());
        }
    }

    private boolean verifySignature(String lineSignature, String requestBody) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    props.getChannelSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computedHash = mac.doFinal(requestBody.getBytes(StandardCharsets.UTF_8));
            byte[] receivedHash = Base64.getDecoder().decode(lineSignature);
            return MessageDigest.isEqual(computedHash, receivedHash);
        } catch (IllegalArgumentException e) {
            log.warn("[LINE webhook] malformed signature or empty channel secret", e);
            return false;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("[LINE webhook] signature verification error", e);
            return false;
        }
    }
}
