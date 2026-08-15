package com.merdeleine.payment.utils;

import java.time.Instant;
import java.util.UUID;

public final class MerchantTradeNoGenerator {

    private MerchantTradeNoGenerator() {}

    public static String generate() {
        String epochSeconds = String.valueOf(Instant.now().getEpochSecond());
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 19);
        return "M" + epochSeconds + random; // 30 chars; only letters and digits
    }
}
