
## Catalog Service（商品/檔期/門檻規則）
```mermaid
erDiagram
  PRODUCT ||--o{ PRODUCT_VARIANT : has
  SELL_WINDOW ||--o{ PRODUCT_SELL_WINDOW : config
  PRODUCT ||--o{ PRODUCT_SELL_WINDOW : available_in

  PRODUCT {
    uuid id PK
    string name
    string description
    string status "DRAFT|ACTIVE|INACTIVE"
    datetime created_at
    datetime updated_at
  }

  PRODUCT_VARIANT {
    uuid id PK
    uuid product_id FK
    string sku
    string variant_name
    int price_cents
    string currency
    boolean is_active
  }

  SELL_WINDOW {
    uuid id PK
    string name "e.g. 2026_CNY"
    datetime start_at
    datetime end_at
    string timezone
  }

  PRODUCT_SELL_WINDOW {
    uuid id PK
    uuid product_id FK
    uuid sell_window_id FK
    int threshold_qty "到量門檻"
    int max_total_qty "可選：上限"
    int lead_days "建議生產提前天數"
    int ship_days "建議出貨準備天數"
    boolean enabled
  }
```

## Order Service（訂單/明細/Outbox）
```mermaid
erDiagram
  ORDERS ||--o{ ORDER_ITEM : contains
  ORDERS ||--o{ OUTBOX_EVENT : emits

  ORDERS {
    uuid id PK
    string order_no "human readable"
    uuid customer_id
    uuid sell_window_id
    string status "PENDING_PAYMENT|PAID|CANCELLED|REFUNDED"
    int total_amount_cents
    string currency
    string contact_name
    string contact_phone
    string contact_email
    string shipping_address
    datetime created_at
    datetime updated_at
  }

  ORDER_ITEM {
    uuid id PK
    uuid order_id FK
    uuid product_id
    uuid variant_id
    int quantity
    int unit_price_cents
    int subtotal_cents
  }

  OUTBOX_EVENT {
    uuid id PK
    string aggregate_type "ORDER"
    uuid aggregate_id "order_id"
    string event_type "order.paid.v1 ..."
    json payload
    string status "NEW|SENT|FAILED"
    datetime created_at
    datetime sent_at
  }
```

## Payment Service（付款單/Outbox）
```mermaid
erDiagram
  PAYMENT ||--o{ PAYMENT_TXN : has
  PAYMENT ||--o{ OUTBOX_EVENT : emits

  PAYMENT {
    uuid id PK
    uuid order_id
    string provider "ECpay|Newebpay|LinePay"
    string status "INIT|SUCCEEDED|FAILED|REFUNDED"
    int amount_cents
    string currency
    string provider_payment_id
    datetime created_at
    datetime updated_at
  }

  PAYMENT_TXN {
    uuid id PK
    uuid payment_id FK
    string action "AUTHORIZE|CAPTURE|REFUND"
    string result "OK|NG"
    json raw_response
    datetime created_at
  }

  OUTBOX_EVENT {
    uuid id PK
    string aggregate_type "PAYMENT"
    uuid aggregate_id
    string event_type "payment.succeeded.v1 ..."
    json payload
    string status "NEW|SENT|FAILED"
    datetime created_at
    datetime sent_at
  }
```

## Batch Aggregator / Threshold Service（聚合到量）
```mermaid
erDiagram
  BATCH_COUNTER ||--o{ COUNTER_EVENT_LOG : logs

  BATCH_COUNTER {
    uuid id PK
    uuid sell_window_id
    uuid product_id
    int paid_qty
    int threshold_qty
    string status "OPEN|REACHED|LOCKED"
    datetime reached_at
    uuid reached_event_id "for idempotency"
    datetime updated_at
  }

  COUNTER_EVENT_LOG {
    uuid id PK
    uuid counter_id FK
    string source_event_type
    uuid source_event_id
    int delta_qty
    datetime created_at
  }
```

## Production Planning Service（批次/排程）
```mermaid
erDiagram
  BATCH ||--o{ BATCH_ORDER_LINK : includes
  BATCH ||--o{ BATCH_SCHEDULE : has

  BATCH {
    uuid id PK
    uuid sell_window_id
    uuid product_id
    int target_qty
    string status "CREATED|CONFIRMED|CANCELLED"
    datetime created_at
    datetime confirmed_at
  }

  BATCH_ORDER_LINK {
    uuid id PK
    uuid batch_id FK
    uuid order_id
    int quantity
  }

  BATCH_SCHEDULE {
    uuid id PK
    uuid batch_id FK
    datetime planned_production_date
    datetime planned_ship_date
    string notes
    datetime updated_at
  }
```

## Production Service（工單/狀態）
```mermaid
erDiagram
  WORK_ORDER ||--o{ WORK_STEP : steps

  WORK_ORDER {
    uuid id PK
    uuid batch_id
    string status "READY|IN_PROGRESS|DONE|FAILED"
    datetime start_at
    datetime end_at
    string operator
  }

  WORK_STEP {
    uuid id PK
    uuid work_order_id FK
    string step_name
    string status "TODO|DOING|DONE"
    string notes
  }
```

## Notification Service（通知任務）
```mermaid
erDiagram
  NOTIFICATION_JOB {
    uuid id PK
    string channel "EMAIL|SMS|LINE|SLACK"
    string recipient
    string template_key
    json payload
    string status "REQUESTED|SENT|FAILED"
    int retry_count
    datetime created_at
    datetime sent_at
  }
```
