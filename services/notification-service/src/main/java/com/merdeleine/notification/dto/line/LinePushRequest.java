package com.merdeleine.notification.dto.line;

import java.util.List;

public record LinePushRequest(String to, List<LineTextMessage> messages) {
    public static LinePushRequest of(String to, String text) {
        return new LinePushRequest(to, List.of(LineTextMessage.of(text)));
    }
}
