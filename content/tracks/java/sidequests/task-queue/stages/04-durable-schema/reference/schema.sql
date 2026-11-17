-- The queue's state, with every rule enforced by the database.
CREATE TABLE tasks (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type            text        NOT NULL CHECK (type <> ''),
    payload         text        NOT NULL DEFAULT '',
    status          text        NOT NULL DEFAULT 'ready'
                                CHECK (status IN ('ready', 'leased', 'done', 'dead')),
    attempts        integer     NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    available_at    timestamptz NOT NULL DEFAULT now(),
    lease_until     timestamptz,
    idempotency_key text UNIQUE,
    last_error      text,
    CONSTRAINT lease_only_when_leased CHECK ((status = 'leased') = (lease_until IS NOT NULL))
);

-- The claim query scans the candidates in this order.
CREATE INDEX tasks_ready ON tasks (available_at, id) WHERE status IN ('ready', 'leased');
