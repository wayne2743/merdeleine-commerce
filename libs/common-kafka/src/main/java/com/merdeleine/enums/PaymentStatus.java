package com.merdeleine.enums;

public enum PaymentStatus {
    INIT,
    SUCCEEDED,
    PENDING,     // 已向 PayPal 建單，等待付款
    AUTHORIZED,  // 使用者已授權（可選）
    PAID,        // capture completed
    FAILED,      // 付款失敗
    EXPIRED,     // 超時失效
    CANCELLED    // 使用者取消
 }
