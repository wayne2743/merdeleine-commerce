package com.merdeleine.catalog.service;

import com.merdeleine.catalog.client.OrderServiceClient;
import com.merdeleine.catalog.client.ThresholdServiceClient;
import com.merdeleine.catalog.dto.AutoGroupOrderDtos;
import com.merdeleine.catalog.dto.threshold.BatchCounterRequest;
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
import java.util.Optional;
import java.util.UUID;

@Service
public class AutoGroupOrderService {

    // maxQty 若 catalog 允許 null(不限制)，但 order quota 目前是必填 int，
    // 先用一個大數頂住；後續你要再把 order 端 maxQty 改成 nullable
    private static final int DEFAULT_MAX_QTY = 9999;

    private final ProductRepository productRepository;
    private final SellWindowRepository sellWindowRepository;
    private final ProductSellWindowRepository productSellWindowRepository;
    private final SellWindowPlanner planner;
    private final OrderServiceClient orderClient;
    private final ThresholdServiceClient thresholdClient;

    public AutoGroupOrderService(
            ProductRepository productRepository,
            SellWindowRepository sellWindowRepository,
            ProductSellWindowRepository productSellWindowRepository,
            SellWindowPlanner planner,
            OrderServiceClient orderClient,
            ThresholdServiceClient thresholdClient
    ) {
        this.productRepository = productRepository;
        this.sellWindowRepository = sellWindowRepository;
        this.productSellWindowRepository = productSellWindowRepository;
        this.planner = planner;
        this.orderClient = orderClient;
        this.thresholdClient = thresholdClient;
    }

    @Transactional
    public AutoGroupOrderDtos.Response autoGroupOrder(AutoGroupOrderDtos.Request req) {

        Product product = productRepository.findById(req.productId())
                .orElseThrow(() -> new IllegalArgumentException("product not found: " + req.productId()));

        OffsetDateTime now = OffsetDateTime.now();

        boolean createdNew = false;

        // 1) 找是否已有可用檔期
        Optional<ProductSellWindow> existing = productSellWindowRepository
                .findFirstActiveByProductId(product.getId(), SellWindowStatus.OPEN, now);

        ProductSellWindow psw;
        if (existing.isPresent()) {
            psw = existing.get();
        } else {
            psw = createNewSellWindow(product);
            createdNew = true;
            onProductSellWindowCreated(
                    psw.getSellWindow().getId(),
                    product.getId(),
                    psw.getMinTotalQty()
            );
        }

        SellWindow sw = psw.getSellWindow();

        // 2) 先 upsert quota（確保 order-service 立刻有投影）
        // min/max 以 product_sell_window 設定為準
        int minQty = psw.getMinTotalQty();
        int maxQty = (psw.getMaxTotalQty() == null) ? DEFAULT_MAX_QTY : psw.getMaxTotalQty();

        try {
            orderClient.upsertQuota(new OrderServiceClient.UpsertQuotaRequest(
                    sw.getId(),
                    product.getId(),
                    minQty,
                    maxQty
            ));
        } catch (Exception e) {
            // upsert quota 失敗，如果是新建檔期才補償關閉
            if (createdNew) {
                sw.setStatus(SellWindowStatus.CLOSED);
                psw.setClosed(true);
            }
            throw new RuntimeException("order-service upsert quota failed: " + e.getMessage(), e);
        }

        // 3) 再呼叫 order-service 建 reserved 訂單
        OrderServiceClient.AutoReserveResponse orderResp;
        try {
            orderResp = orderClient.autoReserve(new OrderServiceClient.AutoReserveRequest(
                    sw.getId(),
                    product.getId(),
                    req.qty(),
                    psw.getUnitPriceCents(),
                    psw.getCurrency(),
                    req.contactName(),
                    req.contactPhone(),
                    req.contactEmail(),
                    req.shippingAddress()
            ));
        } catch (Exception e) {
            // auto-reserve 失敗，如果是新建檔期才補償關閉
            if (createdNew) {
                sw.setStatus(SellWindowStatus.CLOSED);
                psw.setClosed(true);
            }
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
        sw.setName("AUTO-" + UUID.randomUUID()); // name unique
        sw.setStartAt(plan.startAt());
        sw.setEndAt(plan.endAt());
        sw.setTimezone(plan.timezone());
        sw.setPaymentTtlMinutes(plan.paymentTtlMinutes());
        sw.setStatus(SellWindowStatus.OPEN);

        sellWindowRepository.save(sw);

        ProductSellWindow psw = new ProductSellWindow();
        psw.setProduct(product);
        psw.setSellWindow(sw);

        // 你後續可改成從 Product 設定 / 預設規則算出來
        psw.setMinTotalQty(1);
        psw.setMaxTotalQty(DEFAULT_MAX_QTY);
        psw.setUnitPriceCents(product.getUnitPriceCents());
        psw.setCurrency(product.getCurrency());
        psw.setClosed(false);

        productSellWindowRepository.save(psw);

        return psw;
    }

    private void onProductSellWindowCreated(UUID sellWindowId, UUID productId, int thresholdQty) {

        BatchCounterRequest req = new BatchCounterRequest();
        req.setSellWindowId(sellWindowId);
        req.setProductId(productId);
        req.setThresholdQty(thresholdQty);
        req.setStatus("OPEN");

        thresholdClient.createBatchCounter(req);
    }
}