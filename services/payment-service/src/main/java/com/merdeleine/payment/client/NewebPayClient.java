package com.merdeleine.payment.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.payment.newebpay.NewebPayCrypto;
import com.merdeleine.payment.newebpay.NewebPayProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class NewebPayClient {

    private final NewebPayProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public NewebPayClient(NewebPayProperties properties,
                          ObjectMapper objectMapper,
                          RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
    }

    public CloseResult refund(String merchantOrderNo, String tradeNo, int amountTwd) {
        properties.requireConfigured();

        Map<String, String> closeData = new LinkedHashMap<>();
        closeData.put("RespondType", "JSON");
        closeData.put("Version", properties.closeVersion());
        closeData.put("Amt", String.valueOf(amountTwd));
        closeData.put("MerchantOrderNo", merchantOrderNo);
        closeData.put("TradeNo", tradeNo);
        closeData.put("TimeStamp", String.valueOf(Instant.now().getEpochSecond()));
        closeData.put("IndexType", "1");
        closeData.put("CloseType", "2");

        String postData = NewebPayCrypto.encrypt(
                NewebPayCrypto.formEncode(closeData),
                properties.hashKey(),
                properties.hashIv()
        );

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("MerchantID_", properties.merchantId());
        form.add("PostData_", postData);

        String rawResponse = restClient.post()
                .uri(properties.closeUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);

        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalStateException("NewebPay Close API returned an empty response");
        }

        return parseCloseResponse(rawResponse, merchantOrderNo, tradeNo, amountTwd);
    }

    public void requireConfigured() {
        properties.requireConfigured();
    }

    CloseResult parseCloseResponse(String rawResponse,
                                   String expectedMerchantOrderNo,
                                   String expectedTradeNo,
                                   int expectedAmountTwd) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String status = root.path("Status").asText();
            String message = root.path("Message").asText();
            JsonNode result = normalizeResult(root.path("Result"));

            if (!"SUCCESS".equals(status)) {
                return new CloseResult(false, status, message, expectedTradeNo, rawResponse);
            }

            String merchantOrderNo = result.path("MerchantOrderNo").asText();
            String tradeNo = result.path("TradeNo").asText();
            int amountTwd = result.path("Amt").asInt(-1);

            if (!properties.merchantId().equals(result.path("MerchantID").asText())
                    || !expectedMerchantOrderNo.equals(merchantOrderNo)
                    || !expectedTradeNo.equals(tradeNo)
                    || expectedAmountTwd != amountTwd) {
                throw new IllegalArgumentException("NewebPay Close response does not match the refund request");
            }

            Map<String, String> checkFields = Map.of(
                    "Amt", String.valueOf(amountTwd),
                    "MerchantID", result.path("MerchantID").asText(),
                    "MerchantOrderNo", merchantOrderNo,
                    "TradeNo", tradeNo
            );
            String expectedCheckCode = NewebPayCrypto.checkCode(
                    checkFields, properties.hashKey(), properties.hashIv()
            );
            String receivedCheckCode = result.path("CheckCode").asText();
            if (!NewebPayCrypto.secureEquals(expectedCheckCode, receivedCheckCode)) {
                throw new IllegalArgumentException("Invalid NewebPay Close response CheckCode");
            }

            return new CloseResult(true, status, message, tradeNo, rawResponse);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid NewebPay Close response", ex);
        }
    }

    private JsonNode normalizeResult(JsonNode result) throws JsonProcessingException {
        if (result.isTextual()) {
            return objectMapper.readTree(result.asText());
        }
        return result;
    }

    public record CloseResult(
            boolean success,
            String status,
            String message,
            String tradeNo,
            String rawResponse
    ) {
    }
}
