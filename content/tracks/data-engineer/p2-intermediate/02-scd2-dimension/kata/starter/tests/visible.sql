-- Each test runs apply_snapshot.sql on the fixture (the snapshot of 2026-11-01).
SELECT dojo.script_eq(
    'a new customer gets a first, current version',
    'apply_snapshot.sql',
    $$SELECT name, city, segment, valid_from, valid_to, is_current
      FROM dim_customer WHERE customer_id = 4$$,
    $$VALUES ('Margaret Hamilton', 'Boston', 'retail', DATE '2026-11-01', DATE '9999-12-31', true)$$
);
