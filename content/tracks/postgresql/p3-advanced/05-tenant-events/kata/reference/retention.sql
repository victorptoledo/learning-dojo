-- The month that starts tomorrow, before its first event arrives.
CREATE TABLE events_2027_01 PARTITION OF events FOR VALUES FROM ('2027-01-01 00:00+00') TO ('2027-02-01 00:00+00');

-- October is past the retention period: a catalog change, not millions of deleted rows.
-- (In production, DETACH PARTITION ... CONCURRENTLY, which cannot run in a transaction block.)
ALTER TABLE events DETACH PARTITION events_2026_10;
DROP TABLE events_2026_10;
