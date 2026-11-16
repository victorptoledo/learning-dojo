-- A session holds back the xmin horizon while it keeps a snapshot (backend_xmin) or a
-- transaction id (backend_xid). Your own session has a snapshot right now: leave it out.
SELECT application_name, state
FROM pg_stat_activity
WHERE pid <> pg_backend_pid()
  AND (backend_xmin IS NOT NULL OR backend_xid IS NOT NULL);
