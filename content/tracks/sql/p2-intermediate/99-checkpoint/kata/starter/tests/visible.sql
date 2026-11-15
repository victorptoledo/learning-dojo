-- Tests of schema.sql run your file, then statements that must succeed or must be rejected.
SELECT dojo.script_ok(
    'creates the four tables with their columns',
    'schema.sql',
    $$SELECT count(*) = 16 FROM information_schema.columns
      WHERE table_schema = 'public'
        AND (table_name, column_name) IN (
            ('venues', 'id'), ('venues', 'name'), ('venues', 'capacity'),
            ('events', 'id'), ('events', 'venue_id'), ('events', 'title'),
            ('events', 'starts_at'), ('events', 'ends_at'),
            ('customers', 'id'), ('customers', 'email'), ('customers', 'name'),
            ('tickets', 'id'), ('tickets', 'event_id'), ('tickets', 'customer_id'),
            ('tickets', 'seat'), ('tickets', 'price'))$$,
    'the tables venues, events, customers and tickets need every column of the README'
);
