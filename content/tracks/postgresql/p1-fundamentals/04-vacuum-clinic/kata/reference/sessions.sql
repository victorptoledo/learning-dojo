-- last_seen changes on every request: leave it out of every index, so its updates can be HOT,
-- and keep 10-20% of each page free for the new versions.
CREATE TABLE sessions (
    id         bigint PRIMARY KEY,
    user_id    bigint NOT NULL,
    token      text NOT NULL UNIQUE,
    last_seen  timestamptz NOT NULL,
    created_at timestamptz NOT NULL
) WITH (fillfactor = 85);

CREATE INDEX sessions_user ON sessions (user_id);
