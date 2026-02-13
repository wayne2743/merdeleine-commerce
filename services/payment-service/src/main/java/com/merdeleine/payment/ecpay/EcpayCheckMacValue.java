// EcpayCheckMacValue.java
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
        // 1) 移除 CheckMacValue
        Map<String, String> filtered = params.entrySet().stream()
                .filter(e -> e.getKey() != null && !"CheckMacValue".equalsIgnoreCase(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() == null ? "" : e.getValue()));

        // 2) 依 key 排序（ASCII）
        TreeMap<String, String> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        sorted.putAll(filtered);

        // 3) 組字串：HashKey=...&k=v&...&HashIV=...
        String raw = "HashKey=" + hashKey + "&" +
                sorted.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("&")) +
                "&HashIV=" + hashIv;

        // 4) URL encode（ECPay 要求接近 .NET 編碼；部分符號需替換）:contentReference[oaicite:6]{index=6}
        String urlEncoded = urlEncodeEcpay(raw).toLowerCase(Locale.ROOT);

        // 5) SHA256 + uppercase
        return sha256(urlEncoded).toUpperCase(Locale.ROOT);
    }

    private static String urlEncodeEcpay(String input) {
        // Java URLEncoder 會把空白變 '+'，ECPay 常用的是 '%20' 形式
        String encoded = URLEncoder.encode(input, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%21", "!")
                .replace("%2A", "*")
                .replace("%28", "(")
                .replace("%29", ")");

        // 綠界文件也提到某些語言需對特定字元做替換以符合規則:contentReference[oaicite:7]{index=7}
        encoded = encoded
                .replace("%2D", "-")
                .replace("%5F", "_")
                .replace("%2E", ".")
                .replace("%21", "!");

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
