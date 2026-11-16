-- search.sql
SELECT dojo.script_ok(
    'doc is a stored generated column',
    'search.sql',
    $$SELECT attgenerated = 's' AND format_type(atttypid, atttypmod) = 'tsvector'
      FROM pg_attribute WHERE attrelid = 'tickets'::regclass AND attname = 'doc'$$,
    'tickets.doc must be a tsvector GENERATED ALWAYS AS (...) STORED'
);

SELECT dojo.script_ok(
    'doc follows changes to the title',
    'search.sql',
    $$SELECT doc @@ to_tsquery('english', 'kangaroo') FROM tickets WHERE id = 7$$,
    'after the title changed, doc does not contain its words',
    after => $$UPDATE tickets SET title = 'Kangaroo mode please' WHERE id = 7$$
);

SELECT dojo.script_ok(
    'searches through an index',
    'search.sql',
    $$SELECT dojo.plan($q$SELECT id FROM tickets WHERE doc @@ websearch_to_tsquery('english', 'printer')$q$) !~ 'Seq Scan'$$,
    'a search on doc reads the whole table: index doc with GIN',
    after => 'ANALYZE tickets'
);

SELECT dojo.script_ok(
    'filters metadata through an index',
    'search.sql',
    $$SELECT dojo.plan($q$SELECT id FROM tickets WHERE metadata @> '{"tags": ["urgent"]}'$q$) !~ 'Seq Scan'$$,
    'a containment filter on metadata reads the whole table: index metadata with GIN',
    after => 'ANALYZE tickets'
);

SELECT dojo.script_ok(
    'stems words and drops stop words',
    'search.sql',
    $$SELECT doc @@ to_tsquery('english', 'export & report') AND NOT doc @@ to_tsquery('simple', 'with')
      FROM tickets WHERE id = 5$$,
    'doc should use the english configuration'
);

-- find_tickets.sql (after search.sql)
SELECT dojo.query_eq(
    'ranks title matches first',
    'find_tickets.sql',
    $$VALUES (8, 'Two-factor login error'), (2, 'Login page shows error 500'),
             (1, 'Cannot log in after password reset')$$,
    ordered => true,
    setup => $$SELECT dojo.run_file('search.sql')$$
);

SELECT dojo.query_eq(
    'only open tickets of pro customers',
    'find_tickets.sql',
    $$VALUES (8, 'Two-factor login error'), (6, 'Login errors on mobile'),
             (1, 'Cannot log in after password reset')$$,
    ordered => true,
    setup => $$SELECT dojo.run_file('search.sql');
                UPDATE tickets SET status = 'open' WHERE id = 6;
                UPDATE tickets SET metadata = '{"customer": {"plan": "free"}}' WHERE id = 2$$
);

-- audit.sql
SELECT dojo.script_eq(
    'records only the changed columns',
    'audit.sql',
    $$SELECT op, ticket_id, changes, actor FROM audit_log$$,
    $$VALUES ('UPDATE', 1, '{"status": {"old": "open", "new": "closed"}, "priority": {"old": 3, "new": 4}}'::jsonb, 'dojo')$$,
    after => $$UPDATE tickets SET status = 'closed', priority = 4 WHERE id = 1$$
);

SELECT dojo.script_eq(
    'records nothing when nothing changed',
    'audit.sql',
    $$SELECT count(*) FROM audit_log$$,
    $$VALUES (0)$$,
    after => $$UPDATE tickets SET title = title WHERE id = 2$$
);

SELECT dojo.script_eq(
    'records nested metadata changes as whole values',
    'audit.sql',
    $$SELECT changes FROM audit_log$$,
    $$VALUES ('{"metadata": {"old": {"customer": {"plan": "free"}, "tags": ["ui"]},
                             "new": {"customer": {"plan": "pro"}, "tags": ["ui"]}}}'::jsonb)$$,
    after => $$UPDATE tickets SET metadata = jsonb_set(metadata, '{customer,plan}', '"pro"') WHERE id = 7$$
);

SELECT dojo.script_eq(
    'records the whole deleted ticket',
    'audit.sql',
    $$SELECT op, ticket_id, changes -> 'deleted' ->> 'title', changes -> 'deleted' ->> 'status' FROM audit_log$$,
    $$VALUES ('DELETE', 7, 'Feature request: dark mode', 'open')$$,
    after => $$DELETE FROM tickets WHERE id = 7$$
);

SELECT dojo.script_ok(
    'sets updated_at on every update',
    'audit.sql',
    $$SELECT updated_at = now() FROM tickets WHERE id = 3$$,
    'updated_at was not set to the time of the update',
    after => $$UPDATE tickets SET priority = 1 WHERE id = 3$$
);

SELECT dojo.script_eq(
    'works with the search column too, and ignores it',
    'audit.sql',
    $$SELECT changes FROM audit_log$$,
    $$VALUES ('{"title": {"old": "Printer error", "new": "Label printer error"}}'::jsonb)$$,
    setup => $$SELECT dojo.run_file('search.sql')$$,
    after => $$UPDATE tickets SET title = 'Label printer error' WHERE id = 9$$
);

-- functions.sql
SELECT dojo.script_eq(
    'lists the urgent open tickets of a plan',
    'functions.sql',
    $$SELECT * FROM open_tickets_for('pro')$$,
    $$VALUES (2, 'Login page shows error 500', 5), (12, 'Billing error after upgrade', 4),
             (1, 'Cannot log in after password reset', 3)$$,
    ordered => true
);

SELECT dojo.script_eq(
    'works for every plan',
    'functions.sql',
    $$SELECT * FROM open_tickets_for('enterprise')$$,
    $$VALUES (10, 'Slow dashboard', 3)$$,
    ordered => true
);

SELECT dojo.script_fails(
    'rejects an unknown plan',
    'functions.sql',
    $$SELECT * FROM open_tickets_for('gold')$$,
    '22023',
    'an unknown plan must raise invalid_parameter_value (22023)'
);

SELECT dojo.script_ok(
    'is marked STABLE',
    'functions.sql',
    $$SELECT provolatile = 's' FROM pg_proc WHERE proname = 'open_tickets_for'$$,
    'the function reads tables: it is STABLE, not VOLATILE or IMMUTABLE'
);
