-- A per-table storage parameter wins over the server setting.
SELECT s.relname,
       s.n_dead_tup,
       round(t.threshold + t.scale_factor * s.reltuples)::bigint AS vacuum_at,
       coalesce(o.enabled, true) = false AS autovacuum_disabled
FROM stats_snapshot AS s
CROSS JOIN LATERAL (
    SELECT max(split_part(opt, '=', 2)) FILTER (WHERE split_part(opt, '=', 1) = 'autovacuum_vacuum_threshold')::bigint AS threshold,
           max(split_part(opt, '=', 2)) FILTER (WHERE split_part(opt, '=', 1) = 'autovacuum_vacuum_scale_factor')::float8 AS scale_factor,
           max(split_part(opt, '=', 2)) FILTER (WHERE split_part(opt, '=', 1) = 'autovacuum_enabled')::boolean AS enabled
    FROM unnest(s.reloptions) AS opt
) AS o
CROSS JOIN LATERAL (
    SELECT coalesce(o.threshold, current_setting('autovacuum_vacuum_threshold')::bigint) AS threshold,
           coalesce(o.scale_factor, current_setting('autovacuum_vacuum_scale_factor')::float8) AS scale_factor
) AS t
WHERE s.n_dead_tup > t.threshold + t.scale_factor * s.reltuples
ORDER BY s.relname;
