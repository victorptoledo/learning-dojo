-- constraints.sql: the clean-up first, then each rule. A booking for tests:
--   INSERT INTO bookings (room_id, organiser, status, during, created_at) VALUES (...)

SELECT dojo.script_eq(
    'cancels the later booking of the double booking',
    'constraints.sql',
    $$SELECT id, status FROM bookings WHERE room_id = 2 ORDER BY id$$,
    $$VALUES (7, 'confirmed'), (9, 'cancelled')$$,
    ordered => true
);

SELECT dojo.script_eq(
    'changes no other booking',
    'constraints.sql',
    $$SELECT count(*) FILTER (WHERE status = 'confirmed'), count(*) FILTER (WHERE status = 'cancelled'), count(*) FROM bookings$$,
    $$VALUES (8, 3, 12)$$
);

SELECT dojo.script_eq(
    'cancels every later overlap, by creation time',
    'constraints.sql',
    $$SELECT id, status FROM bookings WHERE room_id = 5 ORDER BY id$$,
    $$VALUES (13, 'cancelled'), (14, 'confirmed'), (15, 'cancelled')$$,
    ordered => true,
    setup => $$INSERT INTO bookings (room_id, organiser, status, during, created_at) VALUES
                (5, 'ana@example.com', 'confirmed', '[2026-11-18 09:00+00, 2026-11-18 12:00+00)', '2026-11-08 00:00+00'),
                (5, 'bo@example.com', 'confirmed', '[2026-11-18 08:00+00, 2026-11-18 10:00+00)', '2026-11-07 00:00+00'),
                (5, 'cy@example.com', 'confirmed', '[2026-11-18 09:30+00, 2026-11-18 09:45+00)', '2026-11-09 00:00+00')$$
);

SELECT dojo.script_fails(
    'rejects two confirmed bookings of a room that overlap',
    'constraints.sql',
    $$INSERT INTO bookings (room_id, organiser, status, during, created_at)
      VALUES (6, 'ana@example.com', 'confirmed', '[2026-11-16 15:30+00, 2026-11-16 16:30+00)', now())$$,
    '23P01',
    'room 6 is already confirmed from 15:00 to 16:00'
);

SELECT dojo.script_ok(
    'allows back-to-back, cancelled and other-room bookings',
    'constraints.sql',
    $$SELECT count(*) = 15 FROM bookings$$,
    'one of these inserts was rejected',
    after => $$INSERT INTO bookings (room_id, organiser, status, during, created_at) VALUES
                (6, 'ana@example.com', 'confirmed', '[2026-11-16 16:00+00, 2026-11-16 17:00+00)', now()),
                (6, 'bo@example.com', 'cancelled', '[2026-11-16 15:00+00, 2026-11-16 16:00+00)', now()),
                (5, 'cy@example.com', 'confirmed', '[2026-11-16 15:00+00, 2026-11-16 16:00+00)', now())$$
);

SELECT dojo.script_fails(
    'rejects an organiser that is not an email',
    'constraints.sql',
    $$INSERT INTO bookings (room_id, organiser, during, created_at)
      VALUES (5, 'ana at example.com', '[2026-11-20 09:00+00, 2026-11-20 10:00+00)', now())$$,
    '23514',
    'organiser must be an email (a domain with a CHECK)'
);

SELECT dojo.script_fails(
    'rejects an attendee that is not an email',
    'constraints.sql',
    $$INSERT INTO bookings (room_id, organiser, during, attendees, created_at)
      VALUES (5, 'ana@example.com', '[2026-11-20 09:00+00, 2026-11-20 10:00+00)', '{bo@example.com,cy}', now())$$,
    '23514',
    'every attendee must be an email: attendees is an array of the domain'
);

SELECT dojo.script_fails(
    'rejects an unknown status',
    'constraints.sql',
    $$UPDATE bookings SET status = 'pending' WHERE id = 10$$,
    '23514',
    'status must be requested, confirmed or cancelled'
);

SELECT dojo.script_fails(
    'rejects a period that includes its end',
    'constraints.sql',
    $$INSERT INTO bookings (room_id, organiser, during, created_at)
      VALUES (5, 'ana@example.com', '[2026-11-20 09:00+00, 2026-11-20 10:00+00]', now())$$,
    '23514',
    'periods must be half-open, [start, end)'
);

SELECT dojo.script_fails(
    'rejects an empty or endless period',
    'constraints.sql',
    $$INSERT INTO bookings (room_id, organiser, during, created_at)
      VALUES (5, 'ana@example.com', tstzrange('2026-11-20 09:00+00', NULL), now())$$,
    '23514',
    'a booking needs a start and an end'
);

SELECT dojo.script_fails(
    'an empty period is rejected too',
    'constraints.sql',
    $$INSERT INTO bookings (room_id, organiser, during, created_at)
      VALUES (5, 'ana@example.com', tstzrange('2026-11-20 09:00+00', '2026-11-20 09:00+00'), now())$$,
    '23514',
    'an empty range is not a booking'
);

-- free_rooms.sql and busy_hours.sql read the fixture as it is.
SELECT dojo.query_eq(
    'a room is free again when its booking is cancelled',
    'free_rooms.sql',
    $$VALUES (6, 'Evora'), (1, 'Lisbon'), (4, 'Faro')$$,
    ordered => true,
    setup => $$UPDATE bookings SET status = 'cancelled' WHERE id = 3$$
);

SELECT dojo.query_eq(
    'a requested booking does not make a room busy',
    'free_rooms.sql',
    $$VALUES (3, 'Braga'), (6, 'Evora'), (4, 'Faro')$$,
    ordered => true,
    setup => $$UPDATE bookings SET status = 'requested' WHERE id = 5$$
);

SELECT dojo.query_eq(
    'merges touching bookings into busy periods',
    'busy_hours.sql',
    $$VALUES (TIMESTAMPTZ '2026-11-16 09:00+00', TIMESTAMPTZ '2026-11-16 12:00+00'),
             (TIMESTAMPTZ '2026-11-16 13:00+00', TIMESTAMPTZ '2026-11-16 14:30+00')$$,
    ordered => true
);

SELECT dojo.query_eq(
    'only counts confirmed bookings of that day',
    'busy_hours.sql',
    $$VALUES (TIMESTAMPTZ '2026-11-16 09:00+00', TIMESTAMPTZ '2026-11-16 10:30+00'),
             (TIMESTAMPTZ '2026-11-16 11:00+00', TIMESTAMPTZ '2026-11-16 12:00+00'),
             (TIMESTAMPTZ '2026-11-16 13:00+00', TIMESTAMPTZ '2026-11-16 14:30+00')$$,
    ordered => true,
    setup => $$UPDATE bookings SET status = 'cancelled' WHERE id = 2;
                INSERT INTO bookings (room_id, organiser, status, during, created_at)
                VALUES (1, 'ana@example.com', 'confirmed', '[2026-11-17 09:00+00, 2026-11-17 10:00+00)', now())$$
);

-- settings.sql
SELECT dojo.script_eq(
    'creates booking_api with its timeouts',
    'settings.sql',
    $$SELECT r.rolcanlogin, (SELECT array_agg(c ORDER BY c) FROM unnest(s.setconfig) AS c)
      FROM pg_roles AS r
      JOIN pg_db_role_setting AS s ON s.setrole = r.oid AND s.setdatabase = 0
      WHERE r.rolname = 'booking_api'$$,
    $$VALUES (true, '{idle_in_transaction_session_timeout=1min,statement_timeout=5s}'::text[])$$
);

SELECT dojo.script_eq(
    'tunes autovacuum for bookings',
    'settings.sql',
    $$SELECT split_part(o, '=', 1), split_part(o, '=', 2)::float8
      FROM pg_class, unnest(reloptions) AS o WHERE oid = 'bookings'::regclass$$,
    $$VALUES ('autovacuum_vacuum_scale_factor', 0.02), ('autovacuum_analyze_scale_factor', 0.01)$$
);
