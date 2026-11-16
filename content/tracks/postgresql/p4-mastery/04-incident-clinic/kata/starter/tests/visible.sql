-- Each test runs one of your files and checks the rows or the state it leaves behind.
SELECT dojo.query_eq(
    'finds the sequences running out',
    'sequences.sql',
    $$VALUES ('public', 'orders_id_seq', 'integer', 95.0), ('public', 'tickets_id_seq', 'smallint', 91.6)$$,
    ordered => true
);
