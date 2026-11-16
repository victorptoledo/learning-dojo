-- range_agg merges overlapping and adjacent ranges into one multirange; unnest splits it.
SELECT lower(busy) AS starts_at, upper(busy) AS ends_at
FROM (
    SELECT unnest(range_agg(during)) AS busy
    FROM bookings
    WHERE room_id = 1
      AND status = 'confirmed'
      AND during && tstzrange('2026-11-16 00:00+00', '2026-11-17 00:00+00')
) AS merged
ORDER BY starts_at;
