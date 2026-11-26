-- WAL a slot holds back = the primary's position minus the slot's restart_lsn (pg_lsn - pg_lsn
-- gives bytes). A lost slot holds nothing any more, but it is broken: its consumer must be rebuilt.
SELECT s.slot_name,
       s.slot_type,
       s.active,
       round((p.current_lsn - s.restart_lsn) / (1024 * 1024)) AS retained_mb
FROM slots_snapshot AS s
CROSS JOIN primary_status AS p
WHERE NOT s.active
   OR s.wal_status IN ('unreserved', 'lost')
   OR p.current_lsn - s.restart_lsn > 1024 * 1024 * 1024
ORDER BY retained_mb DESC NULLS LAST, s.slot_name;
