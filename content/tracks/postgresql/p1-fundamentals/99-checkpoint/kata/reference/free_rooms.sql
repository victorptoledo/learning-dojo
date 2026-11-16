SELECT r.id, r.name
FROM rooms AS r
WHERE r.capacity >= 8
  AND r.amenities @> ARRAY['projector']
  AND NOT EXISTS (
      SELECT 1 FROM bookings AS b
      WHERE b.room_id = r.id
        AND b.status = 'confirmed'
        AND b.during && tstzrange('2026-11-16 14:00+00', '2026-11-16 15:00+00')
  )
ORDER BY r.capacity, r.name;
