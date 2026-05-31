package com.merdeleine.notification.dto.line;

import java.util.List;

public record LineBroadcastRequest(List<LineTextMessage> messages) {
    public static LineBroadcastRequest of(String text) {
        return new LineBroadcastRequest(List.of(LineTextMessage.of(text)));
    }
}
