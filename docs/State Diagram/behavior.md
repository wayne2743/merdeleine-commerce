## Order 狀態機
```mermaid
stateDiagram-v2
  [*] --> PENDING_PAYMENT : order.created
  PENDING_PAYMENT --> PAID : payment.succeeded
  PENDING_PAYMENT --> CANCELLED : user_cancel / timeout

  PAID --> REFUNDED : refund_approved
  PAID --> CANCELLED : admin_cancel_before_production (optional)

  CANCELLED --> [*]
  REFUNDED --> [*]
```
