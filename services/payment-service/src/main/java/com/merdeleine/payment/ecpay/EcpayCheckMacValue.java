package com.merdeleine.payment.ecpay;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class EcpayCheckMacValue {
    private EcpayCheckMacValue() {}

    public static String gen(Map<String, String> params, String hashKey, String hashIv) {
        // 1) remove CheckMacValue
        Map<String, String> filtered = params.entrySet().stream()
                .filter(e -> e.getKey() != null && !"CheckMacValue".equalsIgnoreCase(e.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() == null ? "" : e.getValue()
                ));

        // 2) ASCII sort (case-sensitive)
        TreeMap<String, String> sorted = new TreeMap<>();
        sorted.putAll(filtered);

        // 3) build raw string
        String raw = "HashKey=" + hashKey + "&" +
                sorted.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("&")) +
                "&HashIV=" + hashIv;

        // 4) URL encode with ECPay rules, then SHA256
        String encoded = urlEncodeEcpay(raw);
        return sha256(encoded).toUpperCase(Locale.ROOT);
    }

    private static String urlEncodeEcpay(String input) {
        String encoded = URLEncoder.encode(input, StandardCharsets.UTF_8);

        // 綠界步驟：先轉小寫 :contentReference[oaicite:2]{index=2}
        encoded = encoded.toLowerCase(Locale.ROOT);

        // 綠界指定替換 :contentReference[oaicite:3]{index=3}
        encoded = encoded
                .replace("%2d", "-")
                .replace("%5f", "_")
                .replace("%2e", ".")
                .replace("%21", "!")
                .replace("%2a", "*")
                .replace("%28", "(")
                .replace("%29", ")");

        // 注意：不要把 '+' 換成 %20（官方範例就是 '+'） :contentReference[oaicite:4]{index=4}
        return encoded;
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
