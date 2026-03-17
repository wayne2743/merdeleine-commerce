package com.merdeleine.gatewaybff.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新用戶完成註冊 — POST /auth/register
 * Google OAuth2 驗證身份後，填寫聯絡資訊以完成帳號建立
 */
public record CompleteRegistrationRequest(

        @NotBlank
        @Size(max = 100)
        String contactName,       // 真實姓名 / 收件人

        @NotBlank
        @Size(max = 30)
        String contactPhone,      // 聯絡電話

        @Email
        @Size(max = 255)
        String contactEmail,      // 聯絡 Email（可不同於登入 Email）

        @Size(max = 1000)
        String shippingAddress    // 預設收件地址
) {}

