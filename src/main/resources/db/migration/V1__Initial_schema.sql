-- 1. Trigger Function for updated_at
CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ language 'plpgsql';

-- 2. Base/Reference Tables (No Dependencies)
CREATE TABLE public.ticket_type (
                             ticket_type VARCHAR(50) PRIMARY KEY,
                             amount DECIMAL(10,2),
                             created_at TIMESTAMP DEFAULT NOW(),
                             updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE public.users (
                       user_id UUID PRIMARY KEY, -- App layer should generate UUIDv7
                       email VARCHAR(255) UNIQUE NOT NULL,
                       phone VARCHAR(15),
                       name VARCHAR(255) NOT NULL,
                       gender CHAR(1) CHECK (gender IN ('M', 'F', 'O')),
                       college_name VARCHAR(255),
                       year_of_study INT,
                       created_at TIMESTAMP DEFAULT NOW(),
                       updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE public.events (
                        event_id UUID PRIMARY KEY,
                        date TIMESTAMP NOT NULL,
                        name VARCHAR(255) NOT NULL,
                        dept_name VARCHAR(255) NOT NULL,
                        reg_count INT DEFAULT 0,
                        attend_count INT DEFAULT 0,
                        event_type VARCHAR(50) NOT NULL CHECK (event_type IN ('TECH', 'NONTECH', 'WORKSHOP', 'HACKATHON', 'RACING')),
                        created_at TIMESTAMP DEFAULT NOW(),
                        updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE public.verification (
                              volunteer_id UUID PRIMARY KEY,
                              email VARCHAR(255) UNIQUE NOT NULL,
                              password_hash VARCHAR(255) NOT NULL,
                              dept VARCHAR(100),
                              name VARCHAR(255) NOT NULL,
                              created_at TIMESTAMP DEFAULT NOW(),
                              updated_at TIMESTAMP DEFAULT NOW()
);

-- 3. Dependent Tables (Level 1)
CREATE TABLE public.ticket_payments (
                                 ticket_id UUID PRIMARY KEY,
                                 user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                 ticket_type VARCHAR(50) NOT NULL REFERENCES ticket_type(ticket_type),
                                 amount_paid DECIMAL(10,2) NOT NULL,
                                 s3_url VARCHAR(512),
                                 status VARCHAR(50) NOT NULL CHECK (status IN ('PendingPayment', 'NotVerified', 'Accepted', 'Rejected')),
                                 email_sent VARCHAR(50) CHECK (email_sent IN ('queued', 'sent')),
                                 created_at TIMESTAMP DEFAULT NOW(),
                                 updated_at TIMESTAMP DEFAULT NOW()
);

-- 4. Dependent Tables (Level 2)
CREATE TABLE public.ticket_event (
                              ticket_id UUID NOT NULL REFERENCES ticket_payments(ticket_id) ON DELETE CASCADE,
                              event_id UUID NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
                              attendance BOOLEAN DEFAULT FALSE,
                              attendance_timestamp TIMESTAMP,
                              created_at TIMESTAMP DEFAULT NOW(),
                              updated_at TIMESTAMP DEFAULT NOW(),
                              PRIMARY KEY (ticket_id, event_id)
);

CREATE TABLE public.hackathon_regs (
                                team_id UUID PRIMARY KEY,
                                team_name VARCHAR(255) UNIQUE NOT NULL,
                                ticket_id UUID NOT NULL REFERENCES ticket_payments(ticket_id) ON DELETE CASCADE,
                                domain VARCHAR(50) NOT NULL CHECK (domain IN ('Software', 'Hardware')),
                                track VARCHAR(100),
                                ps_description TEXT,
                                created_at TIMESTAMP DEFAULT NOW(),
                                updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE public.payment_verification_log (
                                          log_id UUID PRIMARY KEY,
                                          ticket_id UUID NOT NULL REFERENCES ticket_payments(ticket_id) ON DELETE CASCADE,
                                          volunteer_id UUID NOT NULL REFERENCES verification(volunteer_id),
                                          action_taken VARCHAR(50) NOT NULL CHECK (action_taken IN ('Accepted', 'Rejected')),
                                          verif_time TIMESTAMP DEFAULT NOW(),
                                          created_at TIMESTAMP DEFAULT NOW(),
                                          updated_at TIMESTAMP DEFAULT NOW()
);

-- 5. Dependent Tables (Level 3)
CREATE TABLE public.hackathon_members (
                                   member_id UUID PRIMARY KEY,
                                   team_id UUID NOT NULL REFERENCES hackathon_regs(team_id) ON DELETE CASCADE,
                                   is_lead BOOLEAN NOT NULL,
                                   name VARCHAR(255) NOT NULL,
                                   email VARCHAR(255) NOT NULL,
                                   phno VARCHAR(15),
                                   year_of_study INT,
                                   created_at TIMESTAMP DEFAULT NOW(),
                                   updated_at TIMESTAMP DEFAULT NOW(),
                                   UNIQUE (team_id, email)
);

-- 6. Indices
-- Enforces strictly 1 team lead per team using a partial unique index
CREATE UNIQUE INDEX uk_hackathon_team_lead ON public.hackathon_members(team_id) WHERE is_lead = TRUE;

-- Foreign Key Lookup Indices
CREATE INDEX idx_ticket_payments_user_id ON public.ticket_payments(user_id);
CREATE INDEX idx_ticket_payments_ticket_type ON public.ticket_payments(ticket_type);
CREATE INDEX idx_ticket_event_event_id ON public.ticket_event(event_id); -- ticket_id is covered by composite PK
CREATE INDEX idx_hackathon_regs_ticket_id ON public.hackathon_regs(ticket_id);
CREATE INDEX idx_payment_verif_log_ticket_id ON public.payment_verification_log(ticket_id);
CREATE INDEX idx_payment_verif_log_volunteer_id ON public.payment_verification_log(volunteer_id);


-- Optimization Indices for Polling / Dashboard Queries
CREATE INDEX idx_ticket_payments_status ON public.ticket_payments(status);
CREATE INDEX idx_ticket_email_status ON public.ticket_payments(email_sent);
CREATE INDEX idx_events_event_type ON public.events(event_type);
CREATE INDEX idx_hackathon_regs_domain ON public.hackathon_regs(domain);

-- 7. Apply Updated_At Triggers
CREATE TRIGGER update_ticket_type_modtime BEFORE UPDATE ON public.ticket_type FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_users_modtime BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_events_modtime BEFORE UPDATE ON public.events FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_verification_modtime BEFORE UPDATE ON public.verification FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_ticket_payments_modtime BEFORE UPDATE ON public.ticket_payments FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_ticket_event_modtime BEFORE UPDATE ON public.ticket_event FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_hackathon_regs_modtime BEFORE UPDATE ON public.hackathon_regs FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_payment_verif_log_modtime BEFORE UPDATE ON public.payment_verification_log FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_hackathon_members_modtime BEFORE UPDATE ON public.hackathon_members FOR EACH ROW EXECUTE FUNCTION update_modified_column();