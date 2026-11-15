UPDATE jobs
SET status = 'ready',
    locked_by = NULL,
    locked_at = NULL
WHERE status = 'running'
  AND locked_at < TIMESTAMPTZ '2026-11-15 12:00+00' - INTERVAL '5 minutes'
RETURNING id;
