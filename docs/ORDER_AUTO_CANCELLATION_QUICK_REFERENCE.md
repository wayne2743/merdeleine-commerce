# 訂單自動取消工作流 - 快速參考

## 📂 新建文件

### order-service
```
services/order-service/src/main/java/com/merdeleine/order/messaging/
└── SellWindowClosedConsumer.java (新)
    - 監聽 Kafka 主題: sell-window.closed.v1
    - 觸發: OrderService.cancelBySellWindowIdAutomatically()
```

### notification-service
```
services/notification-service/src/main/java/com/merdeleine/notification/messaging/
└── OrderAutoCancelledNotificationConsumer.java (新)
    - 監聽 Kafka 主題: order.auto-cancelled.notification.v1
    - 發送郵件通知

services/notification-service/src/main/resources/templates/
└── order-auto-cancelled.html (新)
    - HTML 郵件模板
    - 主旨: 【merdeleine】訂單已自動取消 - 未達團購門檻
```

## 🔧 修改文件

### order-service
```
OrderService.java (修改)
├─ 新增導入: OrderAutoCancelledNotificationEvent, OffsetDateTime, Arrays
├─ 新增字段: orderAutoCancelledNotificationTopic
├─ 新增方法: cancelBySellWindowIdAutomatically(UUID sellWindowId)
└─ 修改構造函數: 注入 orderAutoCancelledNotificationTopic

OrderRepository.java (修改)
└─ 新增方法: findBySellWindowIdAndStatusIn(UUID, List<OrderStatus>)

application.yml (修改)
├─ 新增配置: app.kafka.topic.order-auto-cancelled-notification-events
└─ 新增配置: merdeleine.kafka.topics.sell-window-closed
```

### notification-service
```
application.yml (修改)
└─ 新增配置: app.kafka.topic.order-auto-cancelled-notification-events
```

## 🔄 事件流

```
1. SellWindow 過期 (scheduled task 每 5 秒檢查)
   ↓
2. SellWindowExpireService.closeExpired()
   ├─ CAS 更新 SellWindow 狀態為 CLOSED
   ├─ 關閉 ProductSellWindow
   ├─ 關閉 OrderQuota
   └─ 寫入 Outbox 事件: SellWindowClosedEvent
   ↓
3. Outbox 發佈器 (5 秒間隔)
   └─ 發佈 SellWindowClosedEvent → Kafka (sell-window.closed.v1)
   ↓
4. order-service: SellWindowClosedConsumer
   └─ 消費 SellWindowClosedEvent
   ↓
5. OrderService.cancelBySellWindowIdAutomatically()
   ├─ SELECT * FROM orders WHERE sell_window_id = ? AND status IN ('RESERVED', 'PAYMENT_REQUESTED')
   ├─ 逐個訂單:
   │  ├─ 設置狀態: CANCELLED
   │  ├─ 釋放配額: quotaService.release()
   │  └─ 寫入 Outbox: OrderAutoCancelledNotificationEvent
   └─ 交易提交
   ↓
6. Outbox 發佈器
   └─ 發佈 OrderAutoCancelledNotificationEvent → Kafka (order.auto-cancelled.notification.v1)
   ↓
7. notification-service: OrderAutoCancelledNotificationConsumer
   ├─ 去重檢查 (notification_job 表)
   ├─ 查詢客戶信息 (API Gateway)
   ├─ 生成郵件內容
   ├─ 發送 HTML 郵件
   ├─ 更新 notification_job 狀態: SENT
   └─ 確認 Kafka 消息
   ↓
8. ✅ 完成 - 客戶收到郵件通知
```

## 🧪 測試步驟

### 前置條件
- 所有微服務正在運行
- Kafka broker 正在運行
- PostgreSQL 正在運行

### 測試流程
1. **創建 SellWindow**
   ```bash
   POST /api/catalog/sell-windows
   Body: {
     "name": "測試開團",
     "startAt": "2026-05-16T00:00:00Z",
     "endAt": "2026-05-16T14:00:00Z"
   }
   # 返回 sellWindowId (假設為 abc-123)
   ```

2. **創建 ProductSellWindow**
   ```bash
   POST /api/catalog/product-sell-windows
   Body: {
     "sellWindowId": "abc-123",
     "productId": "prod-456",
     "thresholdQty": 10,  # 團購門檻
     "maxTotalQty": 100
   }
   ```

3. **創建訂單 (RESERVED)**
   ```bash
   POST /api/orders
   Body: {
     "customerId": "customer-123",
     "sellWindowId": "abc-123",
     "productId": "prod-456",
     "quantity": 5,  # 少於門檻 (10)
     "unitPriceCents": 10000
   }
   # 返回 orderId (假設為 order-789)
   ```

4. **驗證訂單初始狀態**
   ```bash
   GET /api/orders/order-789
   # 應返回 status: "RESERVED"
   ```

5. **觸發 SellWindow 過期**
   ```bash
   # 方式 1: 等待 scheduler 自動執行 (每 5 秒檢查)
   # 或
   # 方式 2: 手動調用 (如果有管理 API)
   POST /api/catalog/admin/close-expired
   ```

6. **驗證訂單自動取消**
   ```bash
   GET /api/orders/order-789
   # 應返回 status: "CANCELLED"
   ```

7. **驗證郵件發送**
   ```
   # 檢查 notification_job 表
   SELECT * FROM notification_job 
   WHERE payload->>'orderId' = 'order-789' 
   AND template_key = 'order-auto-cancelled'
   AND status = 'SENT';
   
   # 或檢查 SMTP 日誌/郵件服務確認發送
   ```

8. **驗證配額釋放**
   ```bash
   GET /api/catalog/product-sell-windows/abc-123/prod-456
   # reserved_qty 應該減少 5
   ```

## 🔍 Kafka 主題驗證

### 檢查 SellWindowClosedEvent
```bash
# 使用 kafka-console-consumer
kafka-console-consumer --bootstrap-server 100.113.120.124:9092 \
  --topic sell-window.closed.v1 \
  --from-beginning \
  --property print.value=true \
  --property print.timestamp=true
```

### 檢查 OrderAutoCancelledNotificationEvent
```bash
kafka-console-consumer --bootstrap-server 100.113.120.124:9092 \
  --topic order.auto-cancelled.notification.v1 \
  --from-beginning \
  --property print.value=true
```

## 📊 數據庫檢查

### order-service (order_db)
```sql
-- 檢查訂單狀態
SELECT id, order_no, customer_id, sell_window_id, status, created_at 
FROM orders 
WHERE sell_window_id = 'abc-123' 
ORDER BY created_at DESC;

-- 檢查 Outbox 事件
SELECT id, aggregate_id, event_type, status, created_at 
FROM outbox_event 
WHERE aggregate_id = 'abc-123' 
AND event_type IN ('sell-window.closed.v1', 'order.auto-cancelled.notification.v1')
ORDER BY created_at DESC;
```

### notification-service (notification_db)
```sql
-- 檢查郵件發送記錄
SELECT id, channel, recipient, template_key, status, sent_at, created_at
FROM notification_job
WHERE template_key = 'order-auto-cancelled'
ORDER BY created_at DESC;

-- 查看郵件內容
SELECT payload 
FROM notification_job
WHERE template_key = 'order-auto-cancelled'
LIMIT 1;
```

## 🐛 故障排除

### 症狀 1: 訂單未自動取消
**檢查項**:
1. SellWindow 是否正確過期 (檢查 SellWindow.status = 'CLOSED')
2. Kafka 消費者是否接收 SellWindowClosedEvent
3. order-service 日誌是否有錯誤
4. OrderService.cancelBySellWindowIdAutomatically() 是否被調用

### 症狀 2: 郵件未發送
**檢查項**:
1. notification_job 表中是否有記錄 (status = 'FAILED')
2. notification-service 日誌中是否有 SMTP 錯誤
3. 客戶郵箱配置是否正確
4. API Gateway 客戶查詢是否失敗
5. 去重檢查是否誤判 (existsByOrderIdAndTemplateKeyAndChannel)

### 症狀 3: 重複郵件
**檢查項**:
1. Kafka 消費者是否正確 commit offset
2. notification_job 去重查詢是否有 bug
3. 檢查 Outbox 是否重複發佈事件

## 📞 相關配置速查

| 組件 | 配置項 | 值 | 說明 |
|------|--------|-----|------|
| Kafka Topic | `merdeleine.kafka.topics.sell-window-closed` | `sell-window.closed.v1` | catalog → order |
| Kafka Topic | `app.kafka.topic.order-auto-cancelled-notification-events` | `order.auto-cancelled.notification.v1` | order → notification |
| Kafka Consumer | Consumer Group (order) | `order-service` | order-service 消費者組 |
| Kafka Consumer | Consumer Group (notification) | `notification-service-group` | notification-service 消費者組 |
| Ack Mode | `listener.ack-mode` | `manual_immediate` | 手動確認，立即返回 |
| Email Template | `template_key` | `order-auto-cancelled` | 對應 `order-auto-cancelled.html` |

