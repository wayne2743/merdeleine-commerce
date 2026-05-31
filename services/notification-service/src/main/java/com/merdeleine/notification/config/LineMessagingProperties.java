package com.merdeleine.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("line.messaging")
public class LineMessagingProperties {

    private String channelAccessToken = "";
    private String channelSecret = "";
    private String webhookPath = "/api/line/webhook";

    public String getChannelAccessToken() { return channelAccessToken; }
    public void setChannelAccessToken(String channelAccessToken) { this.channelAccessToken = channelAccessToken; }

    public String getChannelSecret() { return channelSecret; }
    public void setChannelSecret(String channelSecret) { this.channelSecret = channelSecret; }

    public String getWebhookPath() { return webhookPath; }
    public void setWebhookPath(String webhookPath) { this.webhookPath = webhookPath; }
}
