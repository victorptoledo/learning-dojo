SELECT queue,
       count(*) FILTER (WHERE status = 'ready' AND run_at <= TIMESTAMPTZ '2026-11-15 12:00+00') AS due,
       count(*) FILTER (WHERE status = 'ready' AND run_at > TIMESTAMPTZ '2026-11-15 12:00+00') AS scheduled,
       count(*) FILTER (WHERE status = 'running') AS running,
       count(*) FILTER (WHERE status = 'dead') AS dead
FROM jobs
GROUP BY queue
ORDER BY queue;
