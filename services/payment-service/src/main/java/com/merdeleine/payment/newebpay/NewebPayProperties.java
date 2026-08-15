package com.merdeleine.payment.newebpay;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "newebpay")
public record NewebPayProperties(
        String env,
        String merchantId,
        String hashKey,
        String hashIv,
        String publicBaseUrl,
        String frontendReturnUrl,
        String mpgVersion,
        String closeVersion,
        String stageMpgUrl,
        String prodMpgUrl,
        String stageCloseUrl,
        String prodCloseUrl
) {
    public String mpgUrl() {
        return isProduction() ? prodMpgUrl : stageMpgUrl;
    }

    public String closeUrl() {
        return isProduction() ? prodCloseUrl : stageCloseUrl;
    }

    public String callbackBaseUrl() {
        requireConfigured();
        return publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    public void requireConfigured() {
        if (isBlank(merchantId) || isBlank(hashKey) || isBlank(hashIv) || isBlank(publicBaseUrl)) {
            throw new IllegalStateException(
                    "NewebPay is not configured. Set NEWEBPAY_MERCHANT_ID, NEWEBPAY_HASH_KEY, " +
                            "NEWEBPAY_HASH_IV and PAYMENT_PUBLIC_BASE_URL"
            );
        }
        if (hashKey.getBytes(StandardCharsets.UTF_8).length != 32) {
            throw new IllegalStateException("NEWEBPAY_HASH_KEY must be exactly 32 bytes");
        }
        if (hashIv.getBytes(StandardCharsets.UTF_8).length != 16) {
            throw new IllegalStateException("NEWEBPAY_HASH_IV must be exactly 16 bytes");
        }
    }

    private boolean isProduction() {
        return "prod".equalsIgnoreCase(env);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
