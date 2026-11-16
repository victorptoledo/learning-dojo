-- events: vacuum after 1% of the rows plus 1000 are dead, analyze after 0.5%, and let its
-- workers do five times more work between naps.
ALTER TABLE events SET (
    autovacuum_vacuum_scale_factor = 0.01,
    autovacuum_vacuum_threshold = 1000,
    autovacuum_analyze_scale_factor = 0.005,
    autovacuum_vacuum_cost_limit = 1000
);

-- orders: back to the server's autovacuum, keeping its fillfactor.
ALTER TABLE orders RESET (autovacuum_enabled);
