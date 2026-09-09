ALTER TABLE public.hackathon_members
    DROP CONSTRAINT IF EXISTS hackathon_members_team_id_email_key;

UPDATE public.events SET event_type = 'TECH' where event_id='01a065e6-e1d8-7bfd-8d77-e38e4e756eba'; --pitch it pls

UPDATE public.events SET event_type = 'TECH' where event_id='01a065e6-e1f7-752b-a590-1bfc4f0359a0'; --appflip
ALTER TABLE public.ticket_payments ADD COLUMN payment_id VARCHAR(100) UNIQUE DEFAULT NULL;