package com.merdeleine.enums;

public enum PaymentStatus {
    INIT,
    SUCCEEDED,
    PENDING,     // 已建立金流付款資料，等待付款
    AUTHORIZED,  // 使用者已授權（可選）
    PAID,        // capture completed
    FAILED,      // 付款失敗
    EXPIRED,     // 超時失效
    CANCELLED,   // 使用者取消
    PARTIALLY_REFUNDED,
    REFUNDED
 }
