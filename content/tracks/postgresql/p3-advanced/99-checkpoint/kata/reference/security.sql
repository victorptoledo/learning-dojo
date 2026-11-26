-- Roles for people and the public booking page; none of them owns anything.
CREATE ROLE reception NOLOGIN;
CREATE ROLE clinician NOLOGIN;
CREATE ROLE web NOLOGIN;

GRANT USAGE ON SCHEMA public TO reception, clinician, web;

-- Column privileges: reception never reads a diagnosis or a clinical note.
GRANT SELECT (id, clinic_id, full_name, email) ON patients TO reception;
GRANT SELECT (id, region, clinic_id, patient_id, starts_at, status) ON visits TO reception;
GRANT INSERT ON visits TO reception;

GRANT SELECT ON patients TO clinician;
GRANT SELECT ON visits TO clinician;
GRANT UPDATE (status, notes) ON visits TO clinician;

-- Staff see and write the visits of the clinic of the request, and nothing without one.
-- Grants are on the parent only: the partitions stay closed, since a query on a partition
-- would skip the parent's policy.
ALTER TABLE visits ENABLE ROW LEVEL SECURITY;
CREATE POLICY clinic_staff ON visits
    TO reception, clinician
    USING (clinic_id = nullif(current_setting('app.clinic_id', true), '')::integer)
    WITH CHECK (clinic_id = nullif(current_setting('app.clinic_id', true), '')::integer);

-- One narrow action for the booking page: it runs as the owner, so it fixes its search_path,
-- with pg_temp last so a temporary table cannot stand in for patients or visits.
CREATE FUNCTION next_visit(p_email text, p_after timestamptz) RETURNS timestamptz
LANGUAGE sql STABLE SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
    SELECT min(v.starts_at)
    FROM visits AS v
    JOIN patients AS p ON p.id = v.patient_id
    WHERE lower(p.email) = lower(p_email)
      AND v.status = 'scheduled'
      AND v.starts_at > p_after
$$;

REVOKE EXECUTE ON FUNCTION next_visit(text, timestamptz) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION next_visit(text, timestamptz) TO web;
