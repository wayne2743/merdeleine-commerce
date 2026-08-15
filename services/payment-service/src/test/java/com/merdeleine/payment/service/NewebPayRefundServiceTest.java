package com.merdeleine.payment.service;

import com.merdeleine.enums.PaymentProvider;
import com.merdeleine.enums.PaymentStatus;
import com.merdeleine.payment.client.NewebPayClient;
import com.merdeleine.payment.dto.NewebPayRefundResponse;
import com.merdeleine.payment.entity.Payment;
import com.merdeleine.payment.entity.PaymentRefund;
import com.merdeleine.payment.enums.PaymentRefundStatus;
import com.merdeleine.payment.repository.PaymentRefundRepository;
import com.merdeleine.payment.repository.PaymentRepository;
import com.merdeleine.payment.repository.PaymentTxnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewebPayRefundServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentRefundRepository refundRepository;
    @Mock private PaymentTxnRepository transactionRepository;
    @Mock private NewebPayClient client;

    private NewebPayRefundService service;

    @BeforeEach
    void setUp() {
        service = new NewebPayRefundService(
                paymentRepository, refundRepository, transactionRepository, client
        );
    }

    @Test
    void fullRefundShouldMarkPaymentRefunded() {
        Payment payment = payment();
        when(paymentRepository.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(refundRepository.findByPaymentIdAndIdempotencyKey(payment.getId(), "refund-1"))
                .thenReturn(Optional.empty());
        when(refundRepository.sumSucceededAmountCents(payment.getId())).thenReturn(0L);
        when(refundRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(refundRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(client.refund("M123456789", "24010112345678901", 100))
                .thenReturn(new NewebPayClient.CloseResult(
                        true, "SUCCESS", "退款成功", "24010112345678901", "{\"Status\":\"SUCCESS\"}"
                ));

        NewebPayRefundResponse response = service.refund(payment.getId(), 100, "refund-1");

        assertThat(response.status()).isEqualTo(PaymentRefundStatus.SUCCEEDED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentRepository).save(payment);
        verify(transactionRepository).save(any());
    }

    @Test
    void transportFailureShouldRemainUnknownAndNotChangePaymentStatus() {
        Payment payment = payment();
        when(paymentRepository.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(refundRepository.findByPaymentIdAndIdempotencyKey(payment.getId(), "refund-2"))
                .thenReturn(Optional.empty());
        when(refundRepository.sumSucceededAmountCents(payment.getId())).thenReturn(0L);
        when(refundRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(refundRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(client.refund("M123456789", "24010112345678901", 50))
                .thenThrow(new IllegalStateException("timeout"));

        NewebPayRefundResponse response = service.refund(payment.getId(), 50, "refund-2");

        assertThat(response.status()).isEqualTo(PaymentRefundStatus.UNKNOWN);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void repeatedIdempotencyKeyShouldReturnExistingRefundWithoutCallingProvider() {
        Payment payment = payment();
        PaymentRefund existing = new PaymentRefund();
        existing.setId(UUID.randomUUID());
        existing.setPayment(payment);
        existing.setIdempotencyKey("refund-3");
        existing.setAmountCents(50);
        existing.setStatus(PaymentRefundStatus.SUCCEEDED);

        when(paymentRepository.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(refundRepository.findByPaymentIdAndIdempotencyKey(payment.getId(), "refund-3"))
                .thenReturn(Optional.of(existing));

        NewebPayRefundResponse response = service.refund(payment.getId(), 50, "refund-3");

        assertThat(response.refundId()).isEqualTo(existing.getId());
        assertThat(response.status()).isEqualTo(PaymentRefundStatus.SUCCEEDED);
    }

    private Payment payment() {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(UUID.randomUUID());
        payment.setProvider(PaymentProvider.NewebPay);
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setAmountCents(100);
        payment.setCurrency("TWD");
        payment.setProviderPaymentId("M123456789");
        payment.setProviderCaptureId("24010112345678901");
        return payment;
    }
}
