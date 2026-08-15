package com.merdeleine.enums;

public enum PaymentProvider {
    BankTransfer,
    NewebPay,

    /** Historical value only. No ECPay integration flow remains. */
    @Deprecated
    ECpay,

    /** Historical value only. No PayPal integration flow remains. */
    @Deprecated
    PayPal,

    /** Historical value only. No Line Pay integration flow is configured. */
    @Deprecated
    LinePay,
}
