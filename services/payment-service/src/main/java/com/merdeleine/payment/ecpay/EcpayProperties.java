// EcpayProperties.java
package com.merdeleine.payment.ecpay;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ecpay")
public record EcpayProperties(
        String env,
        String merchantId,
        String hashKey,
        String hashIv,
        String stageUrl,
        String prodUrl,
        String publicBaseUrl
) {
    public String cashierUrl() {
        return "prod".equalsIgnoreCase(env) ? prodUrl : stageUrl;
    }
}
