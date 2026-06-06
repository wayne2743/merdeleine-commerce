package com.merdeleine.notification.client;

import com.merdeleine.notification.config.LineAccountLinkProperties;
import com.merdeleine.notification.dto.line.LineLinkTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Thin client around the LINE Messaging API endpoints used by the account-link flow.
 */
@Component
public class LineMessagingApiClient {

    private static final Logger log = LoggerFactory.getLogger(LineMessagingApiClient.class);

    private final RestClient restClient;
    private final LineAccountLinkProperties props;

    public LineMessagingApiClient(RestClient.Builder restClientBuilder,
                                  LineAccountLinkProperties props) {
        // No base URL: the link-token endpoint is configured as a full URL template.
        this.restClient = restClientBuilder.build();
        this.props = props;
    }

    /**
     * Issues a link token for the given LINE userId.
     * POST https://api.line.me/v2/bot/user/{lineUserId}/linkToken
     */
    public String issueLinkToken(String lineUserId) {
        LineLinkTokenResponse response = restClient.post()
                .uri(props.getLinkTokenApiUrl(), lineUserId)
                .header("Authorization", "Bearer " + props.getChannelAccessToken())
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                    String body;
                    try {
                        body = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        body = "(unreadable)";
                    }
                    log.error("[LINE linkToken] failed: status={}, body={}", res.getStatusCode(), body);
                    throw new IllegalStateException("LINE issue linkToken failed: " + res.getStatusCode());
                })
                .body(LineLinkTokenResponse.class);

        if (response == null || !StringUtils.hasText(response.linkToken())) {
            throw new IllegalStateException("LINE issue linkToken returned empty linkToken");
        }
        return response.linkToken();
    }
}
