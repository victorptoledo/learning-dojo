-- A small theatre: its shows, the numbered seats of each show, the customers and, for the
-- rehearsal rooms of stage 4, the rooms.

CREATE TABLE shows (
    id                integer PRIMARY KEY,
    title             text NOT NULL,
    starts_at         timestamptz NOT NULL,
    standing_capacity integer NOT NULL CHECK (standing_capacity >= 0)
);

CREATE TABLE seats (
    show_id integer NOT NULL REFERENCES shows,
    seat    text NOT NULL,
    PRIMARY KEY (show_id, seat)
);

CREATE TABLE customers (
    id   integer PRIMARY KEY,
    name text NOT NULL UNIQUE
);

CREATE TABLE rooms (
    id   integer PRIMARY KEY,
    name text NOT NULL UNIQUE
);

INSERT INTO shows VALUES
    (1, 'Opening Night', '2026-12-04 20:00+00', 5),
    (2, 'Matinee', '2026-12-05 15:00+00', 3),
    (3, 'Closing Night', '2026-12-06 20:00+00', 0);

INSERT INTO seats
SELECT s.id, r.row_letter || n
FROM shows s, (VALUES ('A'), ('B')) AS r(row_letter), generate_series(1, 5) AS n;

INSERT INTO customers VALUES
    (1, 'Ana'), (2, 'Bia'), (3, 'Caio'), (4, 'Dani'), (5, 'Edu'), (6, 'Fred');

INSERT INTO rooms VALUES (1, 'Studio A'), (2, 'Studio B');
