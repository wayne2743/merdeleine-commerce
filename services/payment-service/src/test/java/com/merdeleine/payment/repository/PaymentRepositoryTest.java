package com.merdeleine.payment.repository;

import com.merdeleine.enums.PaymentProvider;
import com.merdeleine.payment.entity.Payment;
import com.merdeleine.payment.entity.PaymentTxn;
import com.merdeleine.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class PaymentRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("payment_db")
            .withUsername("merdeleine")
            .withPassword("merdeleine_pw");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentTxnRepository paymentTxnRepository;

    private UUID orderId;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        
        testPayment = new Payment();
        testPayment.setId(UUID.randomUUID());
        testPayment.setOrderId(orderId);
        testPayment.setProvider(PaymentProvider.ECpay);
        testPayment.setStatus(PaymentStatus.INIT);
        testPayment.setAmountCents(10000);
        testPayment.setCurrency("TWD");
    }

    @Test
    void testSavePayment() {
        Payment saved = paymentRepository.save(testPayment);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrderId()).isEqualTo(orderId);
        assertThat(saved.getProvider()).isEqualTo(PaymentProvider.ECpay);
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.INIT);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void testFindByOrderId() {
        entityManager.persistAndFlush(testPayment);
        
        Payment payment2 = createPayment(UUID.randomUUID(), PaymentProvider.Newebpay);
        entityManager.persistAndFlush(payment2);
        
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getOrderId()).isEqualTo(orderId);
    }

    @Test
    void testFindByStatus() {
        entityManager.persistAndFlush(testPayment);
        
        Payment succeededPayment = createPayment(UUID.randomUUID(), PaymentProvider.LinePay);
        succeededPayment.setStatus(PaymentStatus.SUCCEEDED);
        entityManager.persistAndFlush(succeededPayment);
        
        List<Payment> initPayments = paymentRepository.findByStatus(PaymentStatus.INIT);
        
        assertThat(initPayments).hasSize(1);
        assertThat(initPayments.get(0).getStatus()).isEqualTo(PaymentStatus.INIT);
    }

    @Test
    void testFindByOrderIdAndStatus() {
        entityManager.persistAndFlush(testPayment);
        
        List<Payment> payments = paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.INIT);
        
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getOrderId()).isEqualTo(orderId);
        assertThat(payments.get(0).getStatus()).isEqualTo(PaymentStatus.INIT);
    }

    @Test
    void testPaymentWithTransactions() {
        Payment saved = entityManager.persistAndFlush(testPayment);
        
        PaymentTxn txn1 = createTransaction(saved);
        PaymentTxn txn2 = createTransaction(saved);
        
        entityManager.persistAndFlush(txn1);
        entityManager.persistAndFlush(txn2);
        entityManager.clear();
        
        List<PaymentTxn> transactions = paymentTxnRepository.findByPaymentId(saved.getId());
        
        assertThat(transactions).hasSize(2);
    }

    @Test
    void testUpdatePaymentStatus() {
        Payment saved = entityManager.persistAndFlush(testPayment);
        saved.setStatus(PaymentStatus.SUCCEEDED);
        saved.setProviderPaymentId("PAY-12345");
        
        Payment updated = paymentRepository.save(saved);
        
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(updated.getProviderPaymentId()).isEqualTo("PAY-12345");
    }

    @Test
    void testDeletePayment() {
        Payment saved = entityManager.persistAndFlush(testPayment);
        UUID paymentId = saved.getId();
        
        paymentRepository.delete(saved);
        entityManager.flush();
        
        assertThat(paymentRepository.findById(paymentId)).isEmpty();
    }

    private Payment createPayment(UUID orderId, PaymentProvider provider) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(orderId);
        payment.setProvider(provider);
        payment.setStatus(PaymentStatus.INIT);
        payment.setAmountCents(10000);
        payment.setCurrency("TWD");
        return payment;
    }

    private PaymentTxn createTransaction(Payment payment) {
        PaymentTxn txn = new PaymentTxn();
        txn.setId(UUID.randomUUID());
        txn.setPayment(payment);
        txn.setAction(com.merdeleine.payment.enums.PaymentTxnAction.AUTHORIZE);
        txn.setResult(com.merdeleine.payment.enums.PaymentTxnResult.OK);
        txn.setRawResponse("{\"status\":\"ok\"}");
        return txn;
    }
}
