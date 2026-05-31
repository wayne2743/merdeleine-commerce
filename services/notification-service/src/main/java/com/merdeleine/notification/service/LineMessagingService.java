package com.merdeleine.notification.service;

import com.merdeleine.notification.config.LineMessagingProperties;
import com.merdeleine.notification.dto.line.LineBroadcastRequest;
import com.merdeleine.notification.dto.line.LinePushRequest;
import com.merdeleine.notification.dto.line.LineReplyRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class LineMessagingService {

    private static final Logger log = LoggerFactory.getLogger(LineMessagingService.class);

    private static final String PUSH_URI      = "/v2/bot/message/push";
    private static final String BROADCAST_URI = "/v2/bot/message/broadcast";
    private static final String REPLY_URI     = "/v2/bot/message/reply";

    private final RestClient lineRestClient;
    private final LineMessagingProperties props;

    public LineMessagingService(@Qualifier("lineRestClient") RestClient lineRestClient,
                                LineMessagingProperties props) {
        this.lineRestClient = lineRestClient;
        this.props = props;
    }

    public void pushMessage(String userId, String text) {
        log.info("[LINE push] userId={}", userId);
        post(PUSH_URI, LinePushRequest.of(userId, text));
    }

    public void broadcastMessage(String text) {
        log.info("[LINE broadcast] length={}", text.length());
        post(BROADCAST_URI, LineBroadcastRequest.of(text));
    }

    public void replyMessage(String replyToken, String text) {
        log.info("[LINE reply] replyToken={}", replyToken);
        post(REPLY_URI, LineReplyRequest.of(replyToken, text));
    }

    private void post(String uri, Object body) {
        lineRestClient.post()
                .uri(uri)
                .header("Authorization", "Bearer " + props.getChannelAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                    String responseBody;
                    try {
                        responseBody = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        responseBody = "(unreadable)";
                    }
                    log.error("[LINE API] {} failed: status={}, body={}", uri, res.getStatusCode(), responseBody);
                    throw new RuntimeException("LINE API call failed: " + res.getStatusCode());
                })
                .toBodilessEntity();
    }
}
