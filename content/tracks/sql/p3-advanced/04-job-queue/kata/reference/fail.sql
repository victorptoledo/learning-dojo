UPDATE jobs
SET status = CASE WHEN attempts >= max_attempts THEN 'dead' ELSE 'ready' END,
    run_at = CASE
                 WHEN attempts >= max_attempts THEN run_at
                 ELSE TIMESTAMPTZ '2026-11-15 12:00+00' + INTERVAL '30 seconds' * power(2, attempts)
             END,
    locked_by = NULL,
    locked_at = NULL,
    last_error = 'smtp timeout'
WHERE id = 7
  AND status = 'running'
RETURNING id, status, run_at;
