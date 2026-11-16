-- Each test runs one of your files and checks the state it leaves behind.
SELECT dojo.script_ok(
    'events is a table partitioned by range',
    'schema.sql',
    $$SELECT relkind = 'p' AND pg_get_partkeydef(oid) = 'RANGE (at)' FROM pg_class WHERE oid = 'events'::regclass$$,
    'events must be PARTITION BY RANGE (at)'
);
