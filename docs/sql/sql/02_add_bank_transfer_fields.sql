-- Migration: Add bank last five digits and transfer date fields to payment table
-- Author: Auto-generated
-- Date: 2026-04-04
-- Description: Add bank_last_five (銀行後五碼) and transfer_at (匯款日期) columns to payment table
--              for tracking ATM transfer information from ECPay payments

-- Add bank_last_five column (5-digit bank account last digits)
ALTER TABLE public.payment
ADD COLUMN IF NOT EXISTS bank_last_five VARCHAR(5);

COMMENT ON COLUMN public.payment.bank_last_five IS '銀行帳號後五碼 (ECPay ATM payment)';

-- Add transfer_at column (when the payment was transferred)
ALTER TABLE public.payment
ADD COLUMN IF NOT EXISTS transfer_at TIMESTAMPTZ;

COMMENT ON COLUMN public.payment.transfer_at IS '匯款日期時間 (ECPay ATM payment callback timestamp)';

-- Ensure the table structure is as expected
-- Optional: verify no existing constraints conflict
DO $$
BEGIN
    -- Log successful migration
    RAISE NOTICE 'Successfully added bank_last_five and transfer_at columns to payment table';
END $$;

