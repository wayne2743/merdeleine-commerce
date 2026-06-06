package com.merdeleine.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LineAccountLinkStartRequest {

    @NotBlank(message = "lineUserId is required")
    @Size(max = 100, message = "lineUserId must not exceed 100 characters")
    private String lineUserId;

    public String getLineUserId() { return lineUserId; }
    public void setLineUserId(String lineUserId) { this.lineUserId = lineUserId; }
}
