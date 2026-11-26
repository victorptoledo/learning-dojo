-- The DELETE removed every payment created before October and, through ON DELETE CASCADE, their
-- refunds. Put back exactly those rows from the recovered copy, parents first, with their own
-- ids (OVERRIDING SYSTEM VALUE), never touching a row that exists now: it may have changed after
-- 14:29, or been deleted on purpose (and then it does not match the accident's condition).
-- Running it again changes nothing.
INSERT INTO payments (id, customer_id, amount, status, created_at)
OVERRIDING SYSTEM VALUE
SELECT r.id, r.customer_id, r.amount, r.status, r.created_at
FROM restored.payments AS r
WHERE r.created_at < '2026-10-01'
ON CONFLICT (id) DO NOTHING;

INSERT INTO refunds (id, payment_id, amount, created_at)
OVERRIDING SYSTEM VALUE
SELECT r.id, r.payment_id, r.amount, r.created_at
FROM restored.refunds AS r
JOIN restored.payments AS p ON p.id = r.payment_id
WHERE p.created_at < '2026-10-01'
ON CONFLICT (id) DO NOTHING;
