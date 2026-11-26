-- How much WAL a statement writes: the insert position before and after it. The write and flush
-- positions (pg_current_wal_lsn, pg_current_wal_flush_lsn) trail behind while the WAL is still in
-- the buffers of an open transaction.
CREATE FUNCTION wal_bytes(statement text) RETURNS numeric
LANGUAGE plpgsql AS $$
DECLARE
    before pg_lsn := pg_current_wal_insert_lsn();
BEGIN
    EXECUTE statement;
    RETURN pg_current_wal_insert_lsn() - before;
END
$$;

-- The raw file can be downloaded again, so its staging table needs neither crash safety nor
-- replicas: unlogged, it writes almost no WAL (and is emptied after a crash, and is empty on
-- standbys).
ALTER TABLE import_staging SET UNLOGGED;
