-- Each test runs one of your files and checks the rows or the state it leaves behind.
SELECT dojo.script_ok(
    'visits is a table partitioned by list of region',
    'schema.sql',
    $$SELECT relkind = 'p' AND pg_get_partkeydef(oid) = 'LIST (region)' FROM pg_class WHERE oid = 'visits'::regclass$$,
    'visits must be PARTITION BY LIST (region)'
);
