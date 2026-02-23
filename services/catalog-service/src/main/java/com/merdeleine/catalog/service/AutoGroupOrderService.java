package com.merdeleine.catalog.service;

import com.merdeleine.catalog.client.OrderServiceClient;

import com.merdeleine.catalog.entity.Product;
import com.merdeleine.catalog.entity.ProductSellWindow;
import com.merdeleine.catalog.entity.SellWindow;
import com.merdeleine.catalog.enums.SellWindowStatus;
import com.merdeleine.catalog.repository.ProductRepository;
import com.merdeleine.catalog.repository.ProductSellWindowRepository;
import com.merdeleine.catalog.repository.SellWindowRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AutoGroupOrderService {

    private final ProductRepository productRepository;
    private final SellWindowRepository sellWindowRepository;
    private final ProductSellWindowRepository productSellWindowRepository;
    private final SellWindowPlanner planner;
    private final OrderServiceClient orderClient;

    public AutoGroupOrderService(
            ProductRepository productRepository,
            SellWindowRepository sellWindowRepository,
            ProductSellWindowRepository productSellWindowRepository,
            SellWindowPlanner planner,
            OrderServiceClient orderClient
    ) {
        this.productRepository = productRepository;
        this.sellWindowRepository = sellWindowRepository;
        this.productSellWindowRepository = productSellWindowRepository;
        this.planner = planner;
        this.orderClient = orderClient;
    }

    @Transactional
    public AutoGroupOrderDtos.Response autoGroupOrder(AutoGroupOrderDtos.Request req) {

        Product product = productRepository.findById(req.productId())
                .orElseThrow(() -> new IllegalArgumentException("product not found: " + req.productId()));

        OffsetDateTime now = OffsetDateTime.now();

        // 1) 找是否已有可用檔期
        ProductSellWindow psw = productSellWindowRepository
                .findFirstActiveByProductId(product.getId(), SellWindowStatus.OPEN, now)
                .orElseGet(() -> createNewSellWindow(product));

        SellWindow sw = psw.getSellWindow();

        // 2) 呼叫 order-service 建 reserved 訂單
        OrderServiceClient.AutoReserveResponse orderResp;
        try {
            orderResp = orderClient.autoReserve(new OrderServiceClient.AutoReserveRequest(
                    sw.getId(),
                    product.getId(),
                    req.qty(),
                    req.contactName(),
                    req.contactPhone(),
                    req.contactEmail(),
                    req.shippingAddress()
            ));
        } catch (Exception e) {
            // MVP：如果這次是新建檔期但下單失敗，做補償：取消檔期 + 關閉 psw
            // （你之後有 BFF/Outbox/補償流程可以再更精緻）
            sw.setStatus(SellWindowStatus.CANCELED);
            psw.setClosed(true);
            // JPA dirty checking 會在 commit 時更新
            throw new RuntimeException("order-service auto-reserve failed: " + e.getMessage(), e);
        }

        return new AutoGroupOrderDtos.Response(
                sw.getId(),
                orderResp.orderId(),
                orderResp.status(),
                new AutoGroupOrderDtos.SellWindowInfo(
                        sw.getStartAt(),
                        sw.getEndAt(),
                        sw.getTimezone(),
                        sw.getPaymentTtlMinutes()
                )
        );
    }

    private ProductSellWindow createNewSellWindow(Product product) {
        SellWindowPlanner.Plan plan = planner.plan();

        SellWindow sw = new SellWindow();
        sw.setName("AUTO-" + UUID.randomUUID()); // 你現在 name unique，先用 UUID 避免衝突
        sw.setStartAt(plan.startAt());
        sw.setEndAt(plan.endAt());
        sw.setTimezone(plan.timezone());
        sw.setPaymentTtlMinutes(plan.paymentTtlMinutes());
        sw.setStatus(SellWindowStatus.OPEN);

        sellWindowRepository.save(sw);

        ProductSellWindow psw = new ProductSellWindow();
        psw.setProduct(product);
        psw.setSellWindow(sw);

        // MVP：先用固定規則
        psw.setMinTotalQty(1);
        psw.setMaxTotalQty(9999);   // 或 null 表示不限制
        psw.setClosed(false);

        productSellWindowRepository.save(psw);

        return psw;
    }
}