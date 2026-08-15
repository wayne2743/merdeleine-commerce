package com.merdeleine.payment.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.payment.newebpay.NewebPayCrypto;
import com.merdeleine.payment.newebpay.NewebPayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NewebPayClientTest {

    private static final String KEY = "12345678901234567890123456789012";
    private static final String IV = "1234567890123456";

    private ObjectMapper objectMapper;
    private NewebPayClient client;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        NewebPayProperties properties = new NewebPayProperties(
                "stage", "MS123456", KEY, IV,
                "https://pay.example.com", "https://shop.example.com/payment/result",
                "2.3", "1.1", "stage-mpg", "prod-mpg", "stage-close", "prod-close"
        );
        client = new NewebPayClient(properties, objectMapper, RestClient.builder());
    }

    @Test
    void shouldVerifySuccessfulCloseResponse() throws Exception {
        String rawResponse = closeResponse(validCheckCode());

        NewebPayClient.CloseResult result = client.parseCloseResponse(
                rawResponse, "M123456789", "24010112345678901", 100
        );

        assertThat(result.success()).isTrue();
        assertThat(result.status()).isEqualTo("SUCCESS");
    }

    @Test
    void shouldRejectInvalidCloseCheckCode() throws Exception {
        String rawResponse = closeResponse("INVALID");

        assertThatThrownBy(() -> client.parseCloseResponse(
                rawResponse, "M123456789", "24010112345678901", 100
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CheckCode");
    }

    private String validCheckCode() {
        return NewebPayCrypto.checkCode(Map.of(
                "Amt", "100",
                "MerchantID", "MS123456",
                "MerchantOrderNo", "M123456789",
                "TradeNo", "24010112345678901"
        ), KEY, IV);
    }

    private String closeResponse(String checkCode) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("MerchantID", "MS123456");
        result.put("MerchantOrderNo", "M123456789");
        result.put("TradeNo", "24010112345678901");
        result.put("Amt", 100);
        result.put("CheckCode", checkCode);
        return objectMapper.writeValueAsString(Map.of(
                "Status", "SUCCESS",
                "Message", "退款成功",
                "Result", result
        ));
    }
}
