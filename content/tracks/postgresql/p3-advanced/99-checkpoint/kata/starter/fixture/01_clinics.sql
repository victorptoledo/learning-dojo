-- A telehealth network with clinics in three regions. Your schema.sql creates the visits table;
-- the Brazilian visits were imported last week into a stand-alone table, visits_br.
-- The tests load this file into a fresh PostgreSQL 17.
CREATE TABLE clinics (
    id     integer PRIMARY KEY,
    name   text NOT NULL CONSTRAINT clinics_name_key UNIQUE,
    region text NOT NULL
);

INSERT INTO clinics (id, name, region) VALUES
    (1, 'Lisboa Centro', 'eu'), (2, 'Porto Norte', 'eu'),
    (3, 'Boston Bay', 'us'), (4, 'Austin Hills', 'us'),
    (5, 'São Paulo Sul', 'br'), (6, 'Recife Mar', 'br');

-- Patient p belongs to clinic 1 + (p - 1) % 6.
CREATE TABLE patients (
    id        integer PRIMARY KEY,
    clinic_id integer NOT NULL REFERENCES clinics (id),
    full_name text NOT NULL,
    email     text NOT NULL,
    diagnosis text
);

INSERT INTO patients (id, clinic_id, full_name, email, diagnosis)
SELECT p, 1 + (p - 1) % 6, 'Patient ' || p, 'patient' || p || '@example.com', 'confidential ' || p
FROM generate_series(2, 3000) AS p;
INSERT INTO patients (id, clinic_id, full_name, email, diagnosis)
VALUES (1, 1, 'Ana Silva', 'Ana.Silva@example.com', 'asthma');

-- The import: 900 visits of the two Brazilian clinics, with the ids they had in the old system.
CREATE TABLE visits_br (
    id         bigint NOT NULL,
    region     text NOT NULL,
    clinic_id  integer NOT NULL,
    patient_id integer NOT NULL,
    starts_at  timestamptz NOT NULL,
    status     text NOT NULL DEFAULT 'scheduled',
    notes      text
);

INSERT INTO visits_br (id, region, clinic_id, patient_id, starts_at, status, notes)
SELECT i, 'br', 5 + i % 2, 5 + i % 2 + 6 * (i % 400), TIMESTAMPTZ '2026-06-01 12:00+00' + i * INTERVAL '6 hours',
       (ARRAY['done', 'cancelled', 'scheduled'])[1 + i % 3], 'imported note ' || i
FROM generate_series(1, 900) AS i;

-- Which clinic owns an address: 20,000 networks that never overlap.
CREATE TABLE clinic_networks (
    id        integer PRIMARY KEY,
    clinic_id integer NOT NULL REFERENCES clinics (id),
    net       cidr NOT NULL
);

INSERT INTO clinic_networks (id, clinic_id, net)
SELECT i, 1 + i % 6, format('10.%s.%s.0/24', i / 256, i % 256)::cidr
FROM generate_series(0, 19999) AS i;

CREATE TABLE prescriptions (
    id         integer PRIMARY KEY,
    patient_id integer NOT NULL REFERENCES patients (id),
    drug       text NOT NULL,
    active     boolean NOT NULL
);

INSERT INTO prescriptions (id, patient_id, drug, active)
SELECT i, 1 + i % 3000, (ARRAY['Amoxicillin', 'Metformin'])[1 + i / 3000], i % 4 <> 0
FROM generate_series(1, 5000) AS i;
INSERT INTO prescriptions (id, patient_id, drug, active)
VALUES (5001, 1, 'Ibuprofen', true), (5002, 1, 'IBUPROFEN', false);

-- The indexes that years of "just add one" left behind.
CREATE INDEX patients_clinic ON patients (clinic_id);
CREATE INDEX patients_clinic_name ON patients (clinic_id, full_name);
CREATE INDEX patients_email_old ON patients (lower(email));
CREATE INDEX prescriptions_patient ON prescriptions (patient_id);
CREATE INDEX prescriptions_patient_id_idx ON prescriptions (patient_id);
CREATE INDEX prescriptions_drug ON prescriptions (drug);
CREATE INDEX prescriptions_drug_active ON prescriptions (drug) WHERE active;
CREATE INDEX clinic_networks_clinic ON clinic_networks (clinic_id);
CREATE INDEX clinic_networks_clinic_net ON clinic_networks (clinic_id, net);

-- Two builds with CREATE INDEX CONCURRENTLY failed halfway in production; this marks them the
-- way the failure left them.
UPDATE pg_index SET indisvalid = false
WHERE indexrelid IN ('patients_email_old'::regclass, 'clinic_networks_clinic_net'::regclass);

-- idx_scan of every index, copied from the primary's pg_stat_user_indexes this morning.
CREATE TABLE index_usage (
    index_name text PRIMARY KEY,
    idx_scan   bigint NOT NULL
);

INSERT INTO index_usage (index_name, idx_scan) VALUES
    ('clinics_pkey', 52000), ('clinics_name_key', 0),
    ('patients_pkey', 0), ('patients_clinic', 310), ('patients_clinic_name', 900),
    ('patients_email_old', 0),
    ('prescriptions_pkey', 800), ('prescriptions_patient', 4100),
    ('prescriptions_patient_id_idx', 0), ('prescriptions_drug', 40),
    ('prescriptions_drug_active', 12),
    ('clinic_networks_pkey', 7), ('clinic_networks_clinic', 0),
    ('clinic_networks_clinic_net', 0),
    ('index_usage_pkey', 3);
