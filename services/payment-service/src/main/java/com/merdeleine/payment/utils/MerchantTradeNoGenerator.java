package com.merdeleine.payment.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class MerchantTradeNoGenerator {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private MerchantTradeNoGenerator() {}

    public static String generate() {
        String timestamp = LocalDateTime.now().format(FORMATTER);

        int random = ThreadLocalRandom.current().nextInt(100, 1000);
        // 100~999，確保一定 3 位數

        return "M" + timestamp + random;
    }
}