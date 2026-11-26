-- Each test runs one of your files and checks the rows or the state it leaves behind.
SELECT dojo.query_eq(
    'lists the slots that need attention',
    'slots.sql',
    $$VALUES ('debezium', 'logical', false, 2304), ('standby_b', 'physical', true, 1344),
             ('old_reports', 'physical', false, 24), ('lost_one', 'physical', false, NULL)$$,
    ordered => true
);
