package com.merdeleine.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.enums.PaymentProvider;
import com.merdeleine.enums.PaymentStatus;
import com.merdeleine.payment.entity.Payment;
import com.merdeleine.payment.newebpay.NewebPayCrypto;
import com.merdeleine.payment.newebpay.NewebPayProperties;
import com.merdeleine.payment.repository.OutboxEventRepository;
import com.merdeleine.payment.repository.PaymentRepository;
import com.merdeleine.payment.repository.PaymentTxnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewebPayServiceTest {

    private static final String KEY = "12345678901234567890123456789012";
    private static final String IV = "1234567890123456";

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentTxnRepository paymentTxnRepository;
    @Mock private OutboxEventRepository outboxEventRepository;

    private NewebPayProperties properties;
    private NewebPayService service;

    @BeforeEach
    void setUp() {
        properties = new NewebPayProperties(
                "stage", "MS123456", KEY, IV,
                "https://pay.example.com", "https://shop.example.com/payment/result",
                "2.3", "1.1",
                "https://ccore.newebpay.com/MPG/mpg_gateway",
                "https://core.newebpay.com/MPG/mpg_gateway",
                "https://ccore.newebpay.com/API/CreditCard/Close",
                "https://core.newebpay.com/API/CreditCard/Close"
        );
        service = new NewebPayService(
                properties, paymentRepository, paymentTxnRepository, outboxEventRepository,
                new ObjectMapper(), "payment.completed.v1", "payment.failed.v1"
        );
    }

    @Test
    void checkoutShouldUsePersistedAmountAndMarkPaymentPending() {
        Payment payment = payment(PaymentStatus.INIT);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        String html = service.buildCheckoutHtml(payment.getId(), "buyer@example.com", "瑪德蓮");

        assertThat(html).contains("https://ccore.newebpay.com/MPG/mpg_gateway")
                .contains("name=\"TradeInfo\"")
                .contains("name=\"TradeSha\"")
                .doesNotContain("buyer@example.com");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentRepository).save(payment);
    }

    @Test
    void twdAmountShouldUsePersistedWholeDollarValueWithoutDividingByOneHundred() {
        assertThat(NewebPayService.toTwdAmount(5, "TWD")).isEqualTo(5);
    }

    @Test
    void successfulCallbackShouldUpdatePaymentAndWriteOneOutboxEvent() throws Exception {
        Payment payment = payment(PaymentStatus.PENDING);
        when(paymentRepository.findByProviderPaymentId("M123456789")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        Map<String, String> form = callbackForm("SUCCESS", "24010112345678901", 100);
        NewebPayService.CallbackResult result = service.handleCallback(form);

        assertThat(result.success()).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(payment.getProviderCaptureId()).isEqualTo("24010112345678901");
        verify(paymentTxnRepository).save(any());
        verify(outboxEventRepository).save(any());
    }

    @Test
    void duplicateSuccessfulCallbackShouldNotWriteTransactionOrOutboxAgain() throws Exception {
        Payment payment = payment(PaymentStatus.SUCCEEDED);
        payment.setProviderCaptureId("24010112345678901");
        when(paymentRepository.findByProviderPaymentId("M123456789")).thenReturn(Optional.of(payment));

        NewebPayService.CallbackResult result = service.handleCallback(
                callbackForm("SUCCESS", "24010112345678901", 100)
        );

        assertThat(result.success()).isTrue();
        verify(paymentRepository, never()).save(any());
        verify(paymentTxnRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    private Map<String, String> callbackForm(String status, String tradeNo, int amountTwd) throws Exception {
        String json = new ObjectMapper().writeValueAsString(Map.of(
                "Status", status,
                "Message", "授權成功",
                "Result", Map.of(
                        "MerchantID", "MS123456",
                        "MerchantOrderNo", "M123456789",
                        "TradeNo", tradeNo,
                        "Amt", amountTwd
                )
        ));
        String tradeInfo = NewebPayCrypto.encrypt(json, KEY, IV);
        return Map.of(
                "MerchantID", "MS123456",
                "TradeInfo", tradeInfo,
                "TradeSha", NewebPayCrypto.tradeSha(tradeInfo, KEY, IV)
        );
    }

    private Payment payment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(UUID.randomUUID());
        payment.setProvider(PaymentProvider.NewebPay);
        payment.setStatus(status);
        payment.setAmountCents(100);
        payment.setCurrency("TWD");
        payment.setProviderPaymentId("M123456789");
        return payment;
    }
}
