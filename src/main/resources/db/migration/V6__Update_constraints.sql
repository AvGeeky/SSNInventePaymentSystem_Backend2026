-- making payment ID non-unique while adding an index

ALTER TABLE public.ticket_payments 
    DROP CONSTRAINT IF EXISTS ticket_payments_payment_id_key;

CREATE INDEX IF NOT EXISTS idx_ticket_payments_payment_id 
    ON public.ticket_payments (payment_id);
