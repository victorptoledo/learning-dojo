-- Each test runs one of your files and checks the rows or the state it leaves behind.
SELECT dojo.script_eq(
    'adds the currency column and the balance check',
    'migration.sql',
    $$SELECT (SELECT format_type(atttypid, atttypmod) || ' ' || attnotnull FROM pg_attribute
              WHERE attrelid = 'accounts'::regclass AND attname = 'currency'),
             (SELECT count(*) FROM accounts WHERE currency = 'EUR'),
             (SELECT convalidated FROM pg_constraint WHERE conrelid = 'accounts'::regclass AND contype = 'c')$$,
    $$VALUES ('character(3) true', 1000, true)$$
);
