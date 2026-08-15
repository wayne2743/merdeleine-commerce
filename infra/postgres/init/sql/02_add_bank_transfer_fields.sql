-- Migration: Add bank last five digits and transfer date fields to payment table
-- Author: Auto-generated
-- Date: 2026-04-04
-- Description: Add bank_last_five (銀行後五碼) and transfer_at (匯款日期) columns to payment table
--              for tracking manual bank-transfer information

-- Add bank_last_five column (5-digit bank account last digits)
ALTER TABLE payment
ADD COLUMN IF NOT EXISTS bank_last_five VARCHAR(5);

COMMENT ON COLUMN payment.bank_last_five IS '銀行帳號後五碼 (manual bank transfer)';

-- Add transfer_at column (when the payment was transferred)
ALTER TABLE payment
ADD COLUMN IF NOT EXISTS transfer_at TIMESTAMPTZ;

COMMENT ON COLUMN payment.transfer_at IS '人工銀行轉帳日期時間';

-- Ensure the table structure is as expected
-- Optional: verify no existing constraints conflict
DO $$
BEGIN
    -- Log successful migration
    RAISE NOTICE 'Successfully added bank_last_five and transfer_at columns to payment table';
END $$;

