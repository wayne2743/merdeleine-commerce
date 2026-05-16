# 訂單自動取消工作流實現總結

## 🎯 整體流程

```
SellWindow 過期
    ↓
SellWindowExpireService.closeExpired() 
    ↓
發布 SellWindowClosedEvent 到 Kafka 主題 (sell-window.closed.v1)
    ↓
order-service 的 SellWindowClosedConsumer 接收事件
    ↓
OrderService.cancelBySellWindowIdAutomatically() 執行取消邏輯
    ├─ 查詢所有 RESERVED 和 PAYMENT_REQUESTED 狀態的訂單
    ├─ 逐個設置為 CANCELLED 狀態
    ├─ 釋放配額（quota）
    └─ 為每個訂單發布 OrderAutoCancelledNotificationEvent
         ↓
發布事件到 Kafka 主題 (order.auto-cancelled.notification.v1)
    ↓
notification-service 的 OrderAutoCancelledNotificationConsumer 接收事件
    ↓
去重檢查（確保同一訂單不重複發送）
    ↓
查詢客戶資訊
    ↓
生成並發送郵件通知客戶
    └─ 郵件主旨：「【merdeleine】訂單已自動取消 - 未達團購門檻」
    └─ 郵件內容說明：「訂購數量不足，商家未能成功開團」
```

## 📝 實現詳情

### 1. order-service 改動

#### 1.1 SellWindowClosedConsumer (新建)
- **路徑**: `services/order-service/src/main/java/com/merdeleine/order/messaging/SellWindowClosedConsumer.java`
- **功能**: 
  - 監聽 Kafka 主題 `sell-window.closed.v1`
  - 消費群組：`order-service`
  - 使用 `manual_immediate` 確認模式
  - 交易同步後確認消息（防止重複消費）
- **核心邏輯**:
  ```java
  @KafkaListener(topics = "${merdeleine.kafka.topics.sell-window-closed}", groupId = "${app.kafka.consumer.group-id}")
  @Transactional
  public void onMessage(SellWindowClosedEvent event, Acknowledgment ack) {
      orderService.cancelBySellWindowIdAutomatically(event.sellWindowId());
      // 交易成功後確認
      TransactionSynchronizationManager.registerSynchronization(...);
  }
  ```

#### 1.2 OrderService 擴展
- **新增依賴**: `orderAutoCancelledNotificationTopic` 配置注入
- **新增方法**: `cancelBySellWindowIdAutomatically(UUID sellWindowId)`
  - 查詢該 sellWindowId 下所有 RESERVED 或 PAYMENT_REQUESTED 狀態的訂單
  - 逐個執行取消：
    - 設置訂單狀態為 CANCELLED
    - 調用 `quotaService.release()` 釋放配額
    - 寫入 outbox 事件 `OrderAutoCancelledNotificationEvent`
  - **冪等性**: 已取消的訂單會被跳過

#### 1.3 OrderRepository 擴展
- **新增方法**: `findBySellWindowIdAndStatusIn(UUID sellWindowId, List<OrderStatus> statuses)`
- 用途：批量查詢指定 sellWindow 且狀態在列表中的訂單

#### 1.4 application.yml 配置更新
```yaml
app:
  kafka:
    topic:
      order-auto-cancelled-notification-events: order.auto-cancelled.notification.v1

merdeleine:
  kafka:
    topics:
      sell-window-closed: sell-window.closed.v1
```

### 2. notification-service 改動

#### 2.1 OrderAutoCancelledNotificationConsumer (新建)
- **路徑**: `services/notification-service/src/main/java/com/merdeleine/notification/messaging/OrderAutoCancelledNotificationConsumer.java`
- **功能**:
  - 監聽 Kafka 主題 `order.auto-cancelled.notification.v1`
  - 消費群組：`notification-service-group`
  - 模板鑰匙：`order-auto-cancelled`

- **核心流程**:
  1. **去重檢查**: 查詢 `notification_job` 表，確認 `(orderId, templateKey, channel)` 組合不存在
  2. **客戶查詢**: 通過 API Gateway 獲取客戶信息（郵件、姓名等）
  3. **郵件發送**: 使用 Thymeleaf 模板引擎渲染 HTML 郵件
  4. **狀態更新**: 根據發送結果更新通知工作記錄

- **去重 SQL 查詢**:
  ```sql
  SELECT EXISTS(
      SELECT 1 FROM notification_job nj
      WHERE nj.channel = :channel
        AND nj.template_key = :templateKey
        AND (nj.payload ->> 'orderId') = :orderId
  )
  ```

#### 2.2 郵件模板 (新建)
- **路徑**: `services/notification-service/src/main/resources/templates/order-auto-cancelled.html`
- **郵件主旨**: 「【merdeleine】訂單已自動取消 - 未達團購門檻」
- **內容要素**:
  - 問候語：「您好 [顧客姓名]，」
  - 取消原因：「訂購數量不足，商家未能成功開團」
  - 訂單信息：訂單編號、訂單 ID、取消時間
  - 退款說明：「3-5 個工作日內原額返還」
  - 客服聯繫提示

#### 2.3 application.yml 配置更新
```yaml
app:
  kafka:
    topic:
      order-auto-cancelled-notification-events: order.auto-cancelled.notification.v1
```

### 3. catalog-service (已完成，無改動)
- `SellWindowExpireService` 已正確使用 `${merdeleine.kafka.topics.sell-window-closed}`
- `SellWindowQuotaOutboxPublisher` 已添加 null-check 保護

## 🔄 事件流數據模型

### SellWindowClosedEvent (catalog → order-service)
```java
record SellWindowClosedEvent(
    UUID eventId,
    String eventType,           // "sell-window.closed.v1"
    UUID sellWindowId,
    UUID productId,              // 可為 null
    OffsetDateTime occurredAt
)
```

### OrderAutoCancelledNotificationEvent (order-service → notification-service)
```java
record OrderAutoCancelledNotificationEvent(
    UUID eventId,
    String eventType,           // "order.auto-cancelled.notification.v1"
    UUID orderId,
    String orderNo,
    UUID customerId,
    UUID sellWindowId,
    String cancelReason,        // "訂購數量不足 而被商家自動取消 沒有開團成功"
    OffsetDateTime occurredAt
)
```

## ✅ 冪等性保證

### 訂單側冪等性
- `OrderService.cancelBySellWindowIdAutomatically()` 檢查訂單狀態
- 已是 CANCELLED 的訂單會被跳過
- 防止多次調用導致配額多次釋放

### 通知側冪等性
- `NotificationJobRepository.existsByOrderIdAndTemplateKeyAndChannel()` 去重
- 同一 `(orderId, templateKey, channel)` 組合只會發送一次郵件
- 防止重複的 Kafka 消息導致重複郵件

## 🔐 故障恢復

### Outbox Pattern
- OrderService 將通知事件寫入本地 `outbox_event` 表
- Outbox 發佈器定期將待發送事件發佈到 Kafka
- 如果發佈失敗，事件保持 PENDING 狀態，後續重試

### Kafka 重新消費
- 如果 notification-service 消費失敗，事件會保留在 Kafka 主題中
- 下次消費者啟動時會從 commit offset 開始消費
- `auto-offset-reset: earliest` 確保不遺漏任何消息

## 📊 編譯驗證

✅ **BUILD SUCCESSFUL** - 所有服務編譯無誤
- order-service: 編譯成功
- notification-service: 編譯成功
- catalog-service: 編譯成功
- threshold-service: 編譯成功

## 🚀 部署檢查清單

- [ ] 確保所有服務的 Kafka 連接正確
- [ ] 驗證 Kafka 主題存在或自動創建已啟用
- [ ] 確認消費群組名稱不衝突
- [ ] 驗證客戶郵箱配置正確（SMTP 服務器、認證等）
- [ ] 確保 order-service 可訪問 quota service（quotaService 依賴）
- [ ] 驗證 notification-service 可訪問 API Gateway（客戶查詢）
- [ ] 執行端到端測試：
  - 創建 SellWindow 並設置過期時間為當前時間
  - 創建訂單（RESERVED 狀態）
  - 觸發 SellWindow 過期機制
  - 驗證訂單自動取消
  - 驗證郵件發送到客戶

## 📬 郵件通知範例

**主旨**: 【merdeleine】訂單已自動取消 - 未達團購門檻

**內容預覽**:
```
您好 [客戶姓名]，

很遺憾地通知您，您在 merdeleine 的訂單已因為未達團購門檻而被自動取消。

✗ 訂單已自動取消
原因：訂購數量不足，商家未能成功開團

訂單編號：[ORDER_NO]
訂單 ID：[ORDER_ID]
取消時間：2026-05-16 14:30

您支付或預留的金額將在 3-5 個工作日內原額返還至原付款帳戶。

如有任何問題或疑慮，歡迎隨時聯繫我們的客服團隊。
— merdeleine.tw
```

