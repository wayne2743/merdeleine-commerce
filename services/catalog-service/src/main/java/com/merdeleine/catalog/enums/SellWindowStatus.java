package com.merdeleine.catalog.enums;

public enum SellWindowStatus {
    DRAFT,
    OPEN,          // 接單中
    PAYMENT_OPEN,  // 已開放付款（confirm 後）
    PAYMENT_CLOSED,
    CLOSED
}