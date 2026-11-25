-- Aggregate each child table on its own before joining (no fan-out), take the month as a
-- half-open range in São Paulo time, and upsert so that a second run converges.
INSERT INTO invoices (customer_id, period, amount)
SELECT c.id,
       DATE '2026-11-01',
       coalesce(f.fees, 0) + coalesce(u.charge, 0)
FROM customers c
LEFT JOIN (
    SELECT s.customer_id, sum(p.monthly_fee) AS fees
    FROM subscriptions s
    JOIN plans p ON p.id = s.plan_id
    GROUP BY s.customer_id
) f ON f.customer_id = c.id
LEFT JOIN (
    SELECT customer_id, round(sum(units) * 0.02, 2) AS charge
    FROM usage
    WHERE recorded_at >= TIMESTAMP '2026-11-01 00:00' AT TIME ZONE 'America/Sao_Paulo'
      AND recorded_at <  TIMESTAMP '2026-12-01 00:00' AT TIME ZONE 'America/Sao_Paulo'
    GROUP BY customer_id
) u ON u.customer_id = c.id
WHERE f.customer_id IS NOT NULL
   OR u.customer_id IS NOT NULL
ON CONFLICT (customer_id, period) DO UPDATE SET amount = EXCLUDED.amount;
