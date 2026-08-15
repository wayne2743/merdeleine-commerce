# NewebPay integration

The payment service supports manual bank transfer and NewebPay MPG credit-card payments.
The former ECPay and PayPal controllers, clients, configuration and checkout page have been removed.

## Required environment variables

```text
NEWEBPAY_ENV=stage
NEWEBPAY_MERCHANT_ID=your-merchant-id
NEWEBPAY_HASH_KEY=32-byte-hash-key
NEWEBPAY_HASH_IV=16-byte-hash-iv
PAYMENT_PUBLIC_BASE_URL=https://public-payment-service.example.com
PAYMENT_FRONTEND_RETURN_URL=https://shop.example.com/payment/result
```

`PAYMENT_PUBLIC_BASE_URL` must be public HTTPS in stage/production so NewebPay can POST to the
NotifyURL and ReturnURL. Never commit MerchantID, HashKey or HashIV.

## API endpoints

### Checkout

```http
GET /payments/newebpay/checkout/{paymentId}?email=buyer@example.com&itemDescription=order
```

The response is an auto-submitting HTML form. The amount and merchant order number are always
loaded from the payment database. In this application, the historically named `amountCents`
field stores whole TWD (for example, `100` means NT$100), so the value is sent to NewebPay
without dividing by 100. It must be a positive integer.

### Callbacks

```text
POST /payments/newebpay/notify
POST /payments/newebpay/return
```

Both callbacks verify `TradeSha`, decrypt `TradeInfo`, and compare MerchantOrderNo and Amt with
the persisted payment. Processing is idempotent; only the first successful callback writes the
payment-completed outbox event. Configure NotifyURL and ReturnURL as different URLs.

### Refund

```http
POST /payments/newebpay/{paymentId}/refunds
Idempotency-Key: a-unique-key-for-this-refund
Content-Type: application/json

{"amountCents": 100}
```

Despite the historical field name, the refund amount is also expressed in whole TWD; the example
above requests a NT$100 refund.

Refunds use NewebPay's credit-card Close API (`CloseType=2`). A full refund changes the payment
to `REFUNDED`; a partial refund changes it to `PARTIALLY_REFUNDED`. A transport or response
verification failure is stored as `UNKNOWN` and must be reconciled before retrying.

## Existing database migration

Run before deploying the updated payment service:

```bash
psql -h <host> -p 5432 -U <user> -d payment_db \
  -f infra/postgres/init/sql/15_add_newebpay_payment.sql
```

Legacy ECPay, PayPal and LinePay provider enum values remain readable for historical payment rows,
but no executable integration flow remains for those providers.
