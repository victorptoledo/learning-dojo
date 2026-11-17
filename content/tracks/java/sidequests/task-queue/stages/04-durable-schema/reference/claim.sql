-- Leases the next task. SKIP LOCKED lets many workers run this at once: a row another
-- worker is claiming is skipped instead of waited for.
UPDATE tasks
SET status = 'leased',
    attempts = attempts + 1,
    lease_until = now() + interval '30 seconds'
WHERE id = (
    SELECT id
    FROM tasks
    WHERE (status = 'ready' AND available_at <= now())
       OR (status = 'leased' AND lease_until < now())
    ORDER BY available_at, id
    LIMIT 1
    FOR UPDATE SKIP LOCKED
)
RETURNING id, type, payload, attempts;
