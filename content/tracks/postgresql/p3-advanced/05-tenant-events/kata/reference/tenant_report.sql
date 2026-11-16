-- No tenant filter here: the policy adds it. The range on at lets the planner prune to one
-- partition.
SELECT kind, count(*) AS events
FROM events
WHERE at >= TIMESTAMPTZ '2026-11-01 00:00+00'
  AND at < TIMESTAMPTZ '2026-12-01 00:00+00'
GROUP BY kind
ORDER BY kind;
