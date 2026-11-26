-- Restore to 2026-11-25 14:29: the newest backup that finished before the target, the segments
-- from the one holding its start to the first one archived at or after the target, and how many
-- of those the archive no longer has. Work with segment numbers, not names: the last 8 hex digits
-- of a name only go up to FF, then the middle 8 go up by one.
WITH target AS (
    SELECT TIMESTAMPTZ '2026-11-25 14:29+00' AS at
),
backup AS (
    SELECT b.label, b.start_lsn
    FROM base_backups AS b, target AS t
    WHERE b.finished_at IS NOT NULL
      AND b.finished_at < t.at
    ORDER BY b.finished_at DESC
    LIMIT 1
),
archive AS (
    -- name = timeline (8 hex) + segment number / 256 (8 hex) + segment number % 256 (8 hex)
    SELECT a.archived_at,
           ('x' || substr(a.segment, 9, 8))::bit(32)::bigint * 256
           + ('x' || substr(a.segment, 17, 8))::bit(32)::bigint AS segno
    FROM wal_archive AS a
),
bounds AS (
    SELECT b.label,
           floor((b.start_lsn - '0/0') / (16 * 1024 * 1024))::bigint AS first_no,
           (SELECT min(a.segno) FROM archive AS a, target AS t WHERE a.archived_at >= t.at) AS last_no
    FROM backup AS b
)
SELECT x.label AS backup,
       pg_walfile_name('0/0'::pg_lsn + x.first_no * 16 * 1024 * 1024) AS first_segment,
       pg_walfile_name('0/0'::pg_lsn + x.last_no * 16 * 1024 * 1024) AS last_segment,
       x.last_no - x.first_no + 1 AS segments,
       (SELECT count(*)
        FROM generate_series(x.first_no, x.last_no) AS g(segno)
        WHERE NOT EXISTS (SELECT 1 FROM archive AS a WHERE a.segno = g.segno)) AS missing
FROM bounds AS x;
