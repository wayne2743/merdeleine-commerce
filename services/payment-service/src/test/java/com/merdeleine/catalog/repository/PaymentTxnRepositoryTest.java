package com.merdeleine.catalog.repository;

import com.merdeleine.catalog.entity.Payment;
import com.merdeleine.catalog.entity.PaymentTxn;
import com.merdeleine.catalog.enums.PaymentProvider;
import com.merdeleine.catalog.enums.PaymentStatus;
import com.merdeleine.catalog.enums.PaymentTxnAction;
import com.merdeleine.catalog.enums.PaymentTxnResult;
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
class PaymentTxnRepositoryTest {

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
    private PaymentTxnRepository paymentTxnRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testPayment = new Payment();
        testPayment.setId(UUID.randomUUID());
        testPayment.setOrderId(UUID.randomUUID());
        testPayment.setProvider(PaymentProvider.ECpay);
        testPayment.setStatus(PaymentStatus.INIT);
        testPayment.setAmountCents(10000);
        testPayment.setCurrency("TWD");
        entityManager.persistAndFlush(testPayment);
    }

    @Test
    void testSavePaymentTxn() {
        PaymentTxn txn = new PaymentTxn();
        txn.setId(UUID.randomUUID());
        txn.setPayment(testPayment);
        txn.setAction(PaymentTxnAction.AUTHORIZE);
        txn.setResult(PaymentTxnResult.OK);
        txn.setRawResponse("{\"status\":\"ok\"}");
        
        PaymentTxn saved = paymentTxnRepository.save(txn);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAction()).isEqualTo(PaymentTxnAction.AUTHORIZE);
        assertThat(saved.getResult()).isEqualTo(PaymentTxnResult.OK);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void testFindByPaymentId() {
        PaymentTxn txn1 = createTransaction(PaymentTxnAction.AUTHORIZE, PaymentTxnResult.OK);
        PaymentTxn txn2 = createTransaction(PaymentTxnAction.CAPTURE, PaymentTxnResult.OK);
        
        entityManager.persistAndFlush(txn1);
        entityManager.persistAndFlush(txn2);
        
        List<PaymentTxn> transactions = paymentTxnRepository.findByPaymentId(testPayment.getId());
        
        assertThat(transactions).hasSize(2);
        assertThat(transactions).extracting(PaymentTxn::getAction)
                .containsExactlyInAnyOrder(PaymentTxnAction.AUTHORIZE, PaymentTxnAction.CAPTURE);
    }

    @Test
    void testPaymentTxnWithDifferentResults() {
        PaymentTxn successTxn = createTransaction(PaymentTxnAction.AUTHORIZE, PaymentTxnResult.OK);
        PaymentTxn failTxn = createTransaction(PaymentTxnAction.AUTHORIZE, PaymentTxnResult.NG);
        
        entityManager.persistAndFlush(successTxn);
        entityManager.persistAndFlush(failTxn);
        
        List<PaymentTxn> transactions = paymentTxnRepository.findByPaymentId(testPayment.getId());
        
        assertThat(transactions).hasSize(2);
        assertThat(transactions).extracting(PaymentTxn::getResult)
                .containsExactlyInAnyOrder(PaymentTxnResult.OK, PaymentTxnResult.NG);
    }

    @Test
    void testDeletePaymentTxn() {
        PaymentTxn txn = createTransaction(PaymentTxnAction.AUTHORIZE, PaymentTxnResult.OK);
        PaymentTxn saved = entityManager.persistAndFlush(txn);
        UUID txnId = saved.getId();
        
        paymentTxnRepository.delete(saved);
        entityManager.flush();
        
        assertThat(paymentTxnRepository.findById(txnId)).isEmpty();
    }

    private PaymentTxn createTransaction(PaymentTxnAction action, PaymentTxnResult result) {
        PaymentTxn txn = new PaymentTxn();
        txn.setId(UUID.randomUUID());
        txn.setPayment(testPayment);
        txn.setAction(action);
        txn.setResult(result);
        txn.setRawResponse("{\"action\":\"" + action + "\",\"result\":\"" + result + "\"}");
        return txn;
    }
}
