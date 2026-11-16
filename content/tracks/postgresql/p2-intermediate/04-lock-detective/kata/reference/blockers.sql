SELECT w.application_name AS waiting,
       b.application_name AS blocked_by,
       b.state AS blocker_state,
       b.query AS blocker_query
FROM pg_stat_activity AS w
CROSS JOIN LATERAL unnest(pg_blocking_pids(w.pid)) AS blocker(pid)
JOIN pg_stat_activity AS b ON b.pid = blocker.pid
ORDER BY waiting, blocked_by;
