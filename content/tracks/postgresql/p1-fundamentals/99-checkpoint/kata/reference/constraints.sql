-- 1. The old system let double bookings through: keep the first confirmed booking of each
--    overlap and cancel the later ones, or the exclusion constraint cannot be added.
UPDATE bookings AS b
SET status = 'cancelled'
WHERE b.status = 'confirmed'
  AND EXISTS (
      SELECT 1 FROM bookings AS e
      WHERE e.room_id = b.room_id
        AND e.status = 'confirmed'
        AND e.during && b.during
        AND (e.created_at, e.id) < (b.created_at, b.id)
  );

-- 2. One rule for every email column.
CREATE DOMAIN email AS text CHECK (VALUE ~ '^[^@[:space:]]+@[^@[:space:]]+$');

ALTER TABLE bookings
    ALTER COLUMN organiser TYPE email,
    ALTER COLUMN attendees TYPE email[];

-- 3. The status list and the shape of a period.
ALTER TABLE bookings
    ADD CONSTRAINT bookings_status CHECK (status IN ('requested', 'confirmed', 'cancelled')),
    ADD CONSTRAINT bookings_during CHECK (NOT isempty(during) AND lower_inc(during) AND NOT upper_inc(during)
                                          AND NOT lower_inf(during) AND NOT upper_inf(during));

-- 4. No two confirmed bookings of one room may overlap. btree_gist gives GiST the = on room_id.
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE bookings
    ADD CONSTRAINT bookings_no_overlap
    EXCLUDE USING gist (room_id WITH =, during WITH &&) WHERE (status = 'confirmed');
