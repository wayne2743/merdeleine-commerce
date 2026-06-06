package com.merdeleine.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("line.messaging-api")
public class LineAccountLinkProperties {

    /** Channel access token used to call the LINE Messaging API (issue link token). */
    private String channelAccessToken = "";

    /** Base URL of LINE's account link dialog. */
    private String accountLinkBaseUrl = "https://access.line.me/dialog/bot/accountLink";

    /** LINE API endpoint template for issuing a link token; contains {lineUserId}. */
    private String linkTokenApiUrl = "https://api.line.me/v2/bot/user/{lineUserId}/linkToken";

    /** How long a generated nonce stays valid, in minutes. */
    private long nonceExpireMinutes = 10;

    public String getChannelAccessToken() { return channelAccessToken; }
    public void setChannelAccessToken(String channelAccessToken) { this.channelAccessToken = channelAccessToken; }

    public String getAccountLinkBaseUrl() { return accountLinkBaseUrl; }
    public void setAccountLinkBaseUrl(String accountLinkBaseUrl) { this.accountLinkBaseUrl = accountLinkBaseUrl; }

    public String getLinkTokenApiUrl() { return linkTokenApiUrl; }
    public void setLinkTokenApiUrl(String linkTokenApiUrl) { this.linkTokenApiUrl = linkTokenApiUrl; }

    public long getNonceExpireMinutes() { return nonceExpireMinutes; }
    public void setNonceExpireMinutes(long nonceExpireMinutes) { this.nonceExpireMinutes = nonceExpireMinutes; }
}
