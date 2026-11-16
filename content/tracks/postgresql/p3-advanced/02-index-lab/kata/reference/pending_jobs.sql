-- status = 'pending' implies the partial index's condition, and it is sorted by run_at.
SELECT id, run_at
FROM jobs
WHERE status = 'pending'
ORDER BY run_at
LIMIT 10;
