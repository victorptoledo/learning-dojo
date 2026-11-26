-- Some tests switch to the roles your security.sql creates, to see what they see. Your files are
-- always read first, as the test user (a superuser); the switch happens just before the check.
GRANT USAGE ON SCHEMA dojo TO PUBLIC;

-- 600 visits in Europe and the USA, plus three of Ana Silva (patient 1, clinic 1).
CREATE FUNCTION pg_temp.load() RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO visits (region, clinic_id, patient_id, starts_at, status)
    SELECT CASE WHEN c <= 2 THEN 'eu' ELSE 'us' END, c, c + 6 * (1 + i % 400),
           TIMESTAMPTZ '2026-12-01 08:00+00' + i * INTERVAL '1 hour',
           (ARRAY['scheduled', 'done', 'cancelled'])[1 + i % 3]
    FROM generate_series(1, 600) AS i, LATERAL (SELECT 1 + i % 4 AS c) AS pick;
    INSERT INTO visits (region, clinic_id, patient_id, starts_at, status, notes) VALUES
        ('eu', 1, 1, '2026-11-20 09:00+00', 'done', 'wheezing, new inhaler'),
        ('eu', 1, 1, '2026-12-01 10:00+00', 'cancelled', NULL),
        ('eu', 1, 1, '2026-12-03 09:00+00', 'scheduled', NULL);
END
$$;

-- Acts as a member of staff of a clinic (NULL: no clinic set).
CREATE FUNCTION pg_temp.as_staff(member text, clinic integer) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    EXECUTE format('SET LOCAL ROLE %I', member);
    IF clinic IS NOT NULL THEN
        PERFORM set_config('app.clinic_id', clinic::text, true);
    END IF;
END
$$;

-- schema.sql
SELECT dojo.script_eq(
    'has a partition per region and no default',
    'schema.sql',
    $$SELECT c.relname, pg_get_expr(c.relpartbound, c.oid)
      FROM pg_inherits AS i JOIN pg_class AS c ON c.oid = i.inhrelid
      WHERE i.inhparent = 'visits'::regclass$$,
    $$VALUES ('visits_br', 'FOR VALUES IN (''br'')'),
             ('visits_eu', 'FOR VALUES IN (''eu'')'),
             ('visits_us', 'FOR VALUES IN (''us'')')$$
);

SELECT dojo.script_eq(
    'the imported visits are part of visits, not copied',
    'schema.sql',
    $$SELECT tableoid::regclass::text, count(*), min(id), max(id) FROM visits GROUP BY 1$$,
    $$VALUES ('visits_br', 900, 1, 900)$$
);

SELECT dojo.script_eq(
    'routes new visits to their region',
    'schema.sql',
    $$SELECT tableoid::regclass::text, count(*) FROM visits WHERE region IN ('eu', 'us') GROUP BY 1$$,
    $$VALUES ('visits_eu', 303), ('visits_us', 300)$$,
    after => 'SELECT pg_temp.load()'
);

SELECT dojo.script_fails(
    'a visit of an unknown region is rejected',
    'schema.sql',
    $$INSERT INTO visits (region, clinic_id, patient_id, starts_at) VALUES ('jp', 1, 1, '2026-12-05 10:00+00')$$,
    '23514',
    'a visit with region jp must fail: there is no partition for it, and no default'
);

SELECT dojo.script_eq(
    'new Brazilian visits get ids after the imported ones',
    'schema.sql',
    $$SELECT id > 900 FROM visits WHERE notes = 'new'$$,
    $$VALUES (true), (true)$$,
    after => $$INSERT INTO visits (region, clinic_id, patient_id, starts_at, notes)
                VALUES ('br', 5, 5, '2026-12-05 10:00+00', 'new'), ('br', 6, 6, '2026-12-05 11:00+00', 'new')$$
);

SELECT dojo.script_fails(
    'every partition checks the clinic, the import too',
    'schema.sql',
    $$INSERT INTO visits (region, clinic_id, patient_id, starts_at) VALUES ('br', 99, 5, '2026-12-05 10:00+00')$$,
    '23503',
    'a visit of clinic 99, which does not exist, must be rejected'
);

SELECT dojo.script_ok(
    'every partition has an index on patient and time',
    'schema.sql',
    $$SELECT count(*) = 3 FROM pg_index AS i JOIN pg_inherits AS h ON h.inhrelid = i.indrelid
      WHERE h.inhparent = 'visits'::regclass
        AND (SELECT array_agg(a.attname::text ORDER BY k.ord)
             FROM unnest(i.indkey::int2[]) WITH ORDINALITY AS k(attnum, ord)
             JOIN pg_attribute AS a ON a.attrelid = i.indrelid AND a.attnum = k.attnum) = '{patient_id,starts_at}'$$,
    'create one index on (patient_id, starts_at) on the parent table: every partition, the attached one too, gets it'
);

SELECT dojo.script_ok(
    'a query on one region reads one partition',
    'schema.sql',
    $$SELECT dojo.plan($q$SELECT count(*) FROM visits WHERE region = 'br' AND status = 'done'$q$)
             !~ '(visits_eu|visits_us)'$$,
    'the planner did not prune: is visits partitioned by list of region?',
    after => 'SELECT pg_temp.load(); ANALYZE visits'
);

-- indexes.sql
SELECT dojo.script_ok(
    'finds the network of an address through an index',
    'indexes.sql',
    $$SELECT dojo.plan($q$SELECT clinic_id FROM clinic_networks WHERE net >>= inet '10.20.30.40'$q$) !~ 'Seq Scan'$$,
    'looking up the network that contains an address reads the whole table: a B-tree cannot answer >>=',
    after => 'ANALYZE clinic_networks'
);

SELECT dojo.script_fails(
    'a second active prescription of a drug is rejected, whatever its case',
    'indexes.sql',
    $$INSERT INTO prescriptions (id, patient_id, drug, active) VALUES (6000, 1, 'ibuprofen', true)$$,
    '23505',
    'patient 1 already has an active Ibuprofen prescription'
);

SELECT dojo.script_eq(
    'inactive and other patients'' prescriptions are allowed',
    'indexes.sql',
    $$SELECT count(*) FROM prescriptions WHERE lower(drug) = 'ibuprofen'$$,
    $$VALUES (5)$$,
    after => $$INSERT INTO prescriptions (id, patient_id, drug, active) VALUES
                (6000, 1, 'Ibuprofen', false), (6001, 2, 'Ibuprofen', true), (6002, 3, 'ibuprofen', true)$$
);

SELECT dojo.script_ok(
    'a prescription can become active again after the old one ends',
    'indexes.sql',
    $$SELECT count(*) = 1 FROM prescriptions WHERE patient_id = 1 AND lower(drug) = 'ibuprofen' AND active AND id = 5002$$,
    'ending the active prescription and activating the old one must work',
    after => $$UPDATE prescriptions SET active = false WHERE id = 5001;
               UPDATE prescriptions SET active = true WHERE id = 5002$$
);

-- index_report.sql
SELECT dojo.query_eq(
    'lists the indexes to drop with their reason',
    'index_report.sql',
    $$VALUES ('clinic_networks_clinic', 'unused'), ('clinic_networks_clinic_net', 'invalid'),
             ('patients_clinic', 'redundant'), ('patients_email_old', 'invalid'),
             ('prescriptions_patient_id_idx', 'redundant')$$,
    ordered => true
);

SELECT dojo.query_eq(
    'a repaired index is no longer reported',
    'index_report.sql',
    $$VALUES ('clinic_networks_clinic', 'redundant'), ('clinic_networks_clinic_net', 'unused'),
             ('patients_clinic', 'redundant'), ('patients_email_old', 'invalid'),
             ('prescriptions_patient_id_idx', 'redundant')$$,
    ordered => true,
    setup => $$UPDATE pg_index SET indisvalid = true WHERE indexrelid = 'clinic_networks_clinic_net'::regclass$$
);

SELECT dojo.query_eq(
    'judges indexes by the catalog, not by their names',
    'index_report.sql',
    $$VALUES ('clinic_networks_clinic', 'unused'), ('clinic_networks_clinic_net', 'invalid'),
             ('patients_clinic', 'redundant'), ('patients_email_old', 'invalid'),
             ('prescriptions_patient', 'redundant'), ('visits_by_region', 'unused')$$,
    ordered => true,
    setup => $$DROP INDEX prescriptions_patient_id_idx;
                CREATE INDEX prescriptions_a_patient ON prescriptions (patient_id);
                CREATE INDEX visits_by_region ON visits_br (region);
                INSERT INTO index_usage VALUES ('prescriptions_a_patient', 5), ('visits_by_region', 0)$$
);

-- security.sql (after schema.sql and the visits)
SELECT dojo.script_eq(
    'reception reads patients without their diagnosis',
    NULL,
    $$SELECT id, full_name, email FROM patients WHERE id = 1$$,
    $$VALUES (1, 'Ana Silva', 'Ana.Silva@example.com')$$,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('security.sql')$$,
    after => $$SELECT pg_temp.as_staff('reception', 1)$$
);

SELECT dojo.script_fails(
    'reception cannot read a diagnosis',
    NULL,
    $$SELECT diagnosis FROM patients WHERE id = 1$$,
    '42501',
    'reception must not read patients.diagnosis',
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('security.sql')$$,
    after => $$SELECT pg_temp.as_staff('reception', 1)$$
);

SELECT dojo.script_fails(
    'reception cannot read clinical notes',
    NULL,
    $$SELECT notes FROM visits$$,
    '42501',
    'reception must not read visits.notes',
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('security.sql')$$,
    after => $$SELECT pg_temp.as_staff('reception', 1)$$
);

SELECT dojo.script_eq(
    'staff see only the visits of their clinic',
    NULL,
    $$SELECT clinic_id, count(*) FROM visits GROUP BY 1$$,
    $$VALUES (5, 450)$$,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT pg_temp.load(); SELECT dojo.run_file('security.sql')$$,
    after => $$SELECT pg_temp.as_staff('clinician', 5)$$
);

SELECT dojo.script_eq(
    'without a clinic staff see nothing',
    NULL,
    $$SELECT count(*) FROM visits$$,
    $$VALUES (0)$$,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT pg_temp.load(); SELECT dojo.run_file('security.sql');
                SELECT set_config('app.clinic_id', '', true)$$,
    after => $$SELECT pg_temp.as_staff('reception', NULL)$$
);

SELECT dojo.script_eq(
    'reception books visits for its own clinic',
    NULL,
    $$SELECT clinic_id, patient_id, status FROM visits WHERE starts_at = '2026-12-09 15:00+00'$$,
    $$VALUES (2, 8, 'scheduled')$$,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('security.sql')$$,
    after => $$SELECT pg_temp.as_staff('reception', 2);
               INSERT INTO visits (region, clinic_id, patient_id, starts_at) VALUES ('eu', 2, 8, '2026-12-09 15:00+00')$$
);

SELECT dojo.script_fails(
    'reception cannot book for another clinic',
    NULL,
    $$INSERT INTO visits (region, clinic_id, patient_id, starts_at) VALUES ('us', 3, 3, '2026-12-09 15:00+00')$$,
    '42501',
    'a receptionist of clinic 2 booked a visit at clinic 3: the policy needs a WITH CHECK',
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('security.sql')$$,
    after => $$SELECT pg_temp.as_staff('reception', 2)$$
);

SELECT dojo.script_eq(
    'a clinician writes the outcome of a visit',
    NULL,
    $$SELECT status, notes FROM visits WHERE patient_id = 1 AND starts_at = '2026-12-03 09:00+00'$$,
    $$VALUES ('done', 'better')$$,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT pg_temp.load(); SELECT dojo.run_file('security.sql')$$,
    after => $$SELECT pg_temp.as_staff('clinician', 1);
               UPDATE visits SET status = 'done', notes = 'better' WHERE patient_id = 1 AND starts_at = '2026-12-03 09:00+00'$$
);

SELECT dojo.script_fails(
    'a clinician cannot move a visit',
    NULL,
    $$UPDATE visits SET starts_at = starts_at + INTERVAL '1 day' WHERE patient_id = 1$$,
    '42501',
    'clinicians may change status and notes only',
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT pg_temp.load(); SELECT dojo.run_file('security.sql')$$,
    after => $$SELECT pg_temp.as_staff('clinician', 1)$$
);

SELECT dojo.script_fails(
    'a partition cannot be read around the policy',
    NULL,
    $$SELECT count(*) FROM visits_br$$,
    '42501',
    'reception read visits_br directly, where the policy of visits does not apply: grant on the parent only',
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('security.sql')$$,
    after => $$SELECT pg_temp.as_staff('reception', 1)$$
);

SELECT dojo.script_eq(
    'the booking page finds the next visit',
    NULL,
    $$SELECT next_visit('ANA.SILVA@example.com', '2026-11-26 00:00+00')$$,
    $$VALUES ('2026-12-03 09:00+00'::timestamptz)$$,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT pg_temp.load(); SELECT dojo.run_file('security.sql')$$,
    after => 'SET LOCAL ROLE web'
);

SELECT dojo.script_fails(
    'the booking page reads no table',
    NULL,
    $$SELECT count(*) FROM patients$$,
    '42501',
    'web may only call next_visit',
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('security.sql')$$,
    after => 'SET LOCAL ROLE web'
);

SELECT dojo.script_fails(
    'only the booking page may call next_visit',
    NULL,
    $$SELECT next_visit('ana.silva@example.com', '2026-11-26 00:00+00')$$,
    '42501',
    'every role (PUBLIC) can execute a new function until you revoke it',
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('security.sql')$$,
    after => $$SELECT pg_temp.as_staff('reception', 1)$$
);

SELECT dojo.script_eq(
    'a temporary table cannot stand in for patients',
    NULL,
    $$SELECT next_visit('ana.silva@example.com', '2026-11-26 00:00+00')$$,
    $$VALUES ('2026-12-03 09:00+00'::timestamptz)$$,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT pg_temp.load(); SELECT dojo.run_file('security.sql');
                CREATE TEMP TABLE patients (id integer, email text);
                INSERT INTO patients VALUES (1, 'nobody@example.com')$$,
    after => 'SET LOCAL ROLE web'
);
