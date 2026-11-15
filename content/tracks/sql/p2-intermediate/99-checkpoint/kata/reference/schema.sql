CREATE TABLE venues (
    id       integer PRIMARY KEY,
    name     text NOT NULL UNIQUE,
    capacity integer NOT NULL CHECK (capacity > 0)
);

CREATE TABLE events (
    id        integer PRIMARY KEY,
    venue_id  integer NOT NULL REFERENCES venues (id),
    title     text NOT NULL,
    starts_at timestamptz NOT NULL,
    ends_at   timestamptz NOT NULL,
    CHECK (ends_at > starts_at)
);

CREATE TABLE customers (
    id    integer PRIMARY KEY,
    email text NOT NULL UNIQUE,
    name  text NOT NULL
);

CREATE TABLE tickets (
    id          integer PRIMARY KEY,
    event_id    integer NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    customer_id integer NOT NULL REFERENCES customers (id) ON DELETE RESTRICT,
    seat        text NOT NULL,
    price       numeric(8, 2) NOT NULL CHECK (price >= 0),
    UNIQUE (event_id, seat)
);

CREATE INDEX tickets_customer_id ON tickets (customer_id);
