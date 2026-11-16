SELECT queryid,
       round(total_exec_time / 1000) AS total_seconds,
       round((100.0 * total_exec_time / sum(total_exec_time) OVER ())::numeric, 1) AS percent_of_total,
       round((total_exec_time / calls)::numeric, 2) AS mean_ms,
       round(100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0), 1) AS hit_percent
FROM statements_snapshot
ORDER BY total_exec_time DESC
LIMIT 3;
