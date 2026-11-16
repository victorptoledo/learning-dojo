-- Each test runs one of your files and checks the rows or the state it leaves behind.
SELECT dojo.script_eq(
    'records the changed columns of an update',
    'audit.sql',
    $$SELECT op, ticket_id, changes FROM audit_log$$,
    $$VALUES ('UPDATE', 3, '{"priority": {"old": 2, "new": 5}}'::jsonb)$$,
    after => $$UPDATE tickets SET priority = 5 WHERE id = 3$$
);
