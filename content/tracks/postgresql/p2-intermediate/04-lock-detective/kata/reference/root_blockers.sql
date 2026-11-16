-- A root blocker blocks someone and waits for no lock itself: ending it releases the chain.
-- An idle-in-transaction session runs no statement, so cancelling would not help.
SELECT b.application_name, pg_terminate_backend(b.pid) AS terminated
FROM pg_stat_activity AS b
WHERE b.pid <> pg_backend_pid()
  AND EXISTS (SELECT 1 FROM pg_stat_activity AS w WHERE b.pid = ANY (pg_blocking_pids(w.pid)))
  AND cardinality(pg_blocking_pids(b.pid)) = 0
ORDER BY b.application_name;
