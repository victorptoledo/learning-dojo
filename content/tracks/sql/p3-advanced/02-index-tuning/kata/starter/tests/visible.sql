-- Tests of indexes.sql run your file, run ANALYZE, then read the plan of a query.
SELECT dojo.script_ok(
    'the email lookup uses an index',
    'indexes.sql',
    $$SELECT dojo.plan($q$SELECT id FROM customers WHERE lower(email) = 'user4242@example.com'$q$)
             !~ '(Seq Scan|Sort|Filter)'
         AND current_setting('enable_seqscan') = 'on'$$,
    'the plan still reads customers with a Seq Scan or a Filter: which expression does the query compare?',
    after => 'ANALYZE'
);
