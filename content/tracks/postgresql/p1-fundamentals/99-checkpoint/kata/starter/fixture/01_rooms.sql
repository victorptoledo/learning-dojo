-- Meeting rooms and their bookings, as the old system left them: no constraints on the
-- bookings yet. The tests load this file into a fresh PostgreSQL 17.
CREATE TABLE rooms (
    id        integer PRIMARY KEY,
    name      text NOT NULL UNIQUE,
    capacity  integer NOT NULL,
    amenities text[] NOT NULL DEFAULT '{}'
);

CREATE TABLE bookings (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    room_id    integer NOT NULL REFERENCES rooms (id),
    organiser  text NOT NULL,
    status     text NOT NULL DEFAULT 'requested',
    during     tstzrange NOT NULL,
    attendees  text[] NOT NULL DEFAULT '{}',
    created_at timestamptz NOT NULL
);

INSERT INTO rooms (id, name, capacity, amenities) VALUES
    (1, 'Lisbon', 12, '{projector,whiteboard}'),
    (2, 'Porto', 6, '{whiteboard}'),
    (3, 'Braga', 8, '{projector}'),
    (4, 'Faro', 20, '{projector,video}'),
    (5, 'Coimbra', 10, '{video}'),
    (6, 'Evora', 8, '{projector,whiteboard}');

INSERT INTO bookings (room_id, organiser, status, during, attendees, created_at) VALUES
    (1, 'ana@example.com', 'confirmed', '[2026-11-16 09:00+00, 2026-11-16 10:30+00)', '{bo@example.com}', '2026-11-01 08:00+00'),
    (1, 'bo@example.com', 'confirmed', '[2026-11-16 10:30+00, 2026-11-16 11:00+00)', '{}', '2026-11-01 09:00+00'),
    (1, 'cy@example.com', 'confirmed', '[2026-11-16 13:00+00, 2026-11-16 14:30+00)', '{ana@example.com,di@example.com}', '2026-11-02 10:00+00'),
    (1, 'di@example.com', 'cancelled', '[2026-11-16 15:00+00, 2026-11-16 16:00+00)', '{}', '2026-11-02 11:00+00'),
    (3, 'ana@example.com', 'confirmed', '[2026-11-16 14:30+00, 2026-11-16 15:30+00)', '{}', '2026-11-03 08:00+00'),
    (4, 'ed@example.com', 'cancelled', '[2026-11-16 14:00+00, 2026-11-16 15:00+00)', '{}', '2026-11-03 09:00+00'),
    (2, 'fi@example.com', 'confirmed', '[2026-11-16 10:00+00, 2026-11-16 11:00+00)', '{gu@example.com}', '2026-11-04 08:00+00'),
    (6, 'gu@example.com', 'confirmed', '[2026-11-16 15:00+00, 2026-11-16 16:00+00)', '{}', '2026-11-04 09:00+00'),
    (2, 'hu@example.com', 'confirmed', '[2026-11-16 10:30+00, 2026-11-16 11:30+00)', '{}', '2026-11-05 08:00+00'),
    (1, 'ed@example.com', 'requested', '[2026-11-16 16:00+00, 2026-11-16 17:00+00)', '{}', '2026-11-05 09:00+00'),
    (4, 'fi@example.com', 'confirmed', '[2026-11-17 14:00+00, 2026-11-17 15:00+00)', '{}', '2026-11-06 08:00+00'),
    (1, 'hu@example.com', 'confirmed', '[2026-11-16 11:00+00, 2026-11-16 12:00+00)', '{}', '2026-11-06 09:00+00');
