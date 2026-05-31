package com.merdeleine.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdminBroadcastRequest {

    @NotBlank(message = "message is required")
    @Size(max = 5000, message = "message must not exceed 5000 characters")
    private String message;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
