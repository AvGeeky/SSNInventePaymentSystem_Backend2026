ALTER TABLE public.ticket_payments
    ADD COLUMN reminder_email_sent VARCHAR(50) CHECK (reminder_email_sent IN ('queued', 'processing', 'sent')) DEFAULT NULL;

ALTER TABLE public.ticket_payments
    ADD COLUMN rejection_email VARCHAR(50) CHECK (rejection_email IN ('queued', 'processing', 'sent')) DEFAULT NULL;

-- Partial index for the reminder email poller
CREATE INDEX idx_tps_reminder_email_null
    ON public.ticket_payments (ticket_id)
    WHERE reminder_email_sent IS NULL AND s3_url IS NULL;

-- Partial index for the rejection email poller
CREATE INDEX idx_tps_rejectionemail_null
    ON public.ticket_payments (ticket_id)
    WHERE ticket_payments.rejection_email IS NULL;

-- Partial index for the primary verified email poller
CREATE INDEX idx_tps_email_sent_null
    ON public.ticket_payments (ticket_id)
    WHERE status = 'Accepted' AND email_sent IS NULL;