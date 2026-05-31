package com.merdeleine.notification.dto.line;

import java.util.List;

public record LineReplyRequest(String replyToken, List<LineTextMessage> messages) {
    public static LineReplyRequest of(String replyToken, String text) {
        return new LineReplyRequest(replyToken, List.of(LineTextMessage.of(text)));
    }
}
