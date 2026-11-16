SELECT datname,
       age(datfrozenxid) AS xid_age,
       round(100.0 * age(datfrozenxid) / current_setting('autovacuum_freeze_max_age')::bigint, 1) AS percent_of_freeze_max_age
FROM pg_database
ORDER BY xid_age DESC, datname;
