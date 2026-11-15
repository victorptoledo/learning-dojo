-- One statement: the subquery locks the chosen row, skipping rows other workers hold, and the
-- UPDATE marks it as ours before anyone else can see it as ready.
UPDATE jobs
SET status = 'running',
    locked_by = 'worker-1',
    locked_at = TIMESTAMPTZ '2026-11-15 12:00+00',
    attempts = attempts + 1
WHERE id = (
    SELECT id
    FROM jobs
    WHERE queue = 'emails'
      AND status = 'ready'
      AND run_at <= TIMESTAMPTZ '2026-11-15 12:00+00'
    ORDER BY priority DESC, run_at, id
    LIMIT 1
    FOR UPDATE SKIP LOCKED
)
RETURNING id, attempts;
