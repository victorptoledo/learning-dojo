-- A job queue in a table. The tests load this file into a fresh PostgreSQL 17.
CREATE TABLE jobs (
    id           bigint PRIMARY KEY,
    queue        text NOT NULL,
    payload      jsonb NOT NULL DEFAULT '{}',
    status       text NOT NULL DEFAULT 'ready' CHECK (status IN ('ready', 'running', 'done', 'dead')),
    priority     integer NOT NULL DEFAULT 0,
    run_at       timestamptz NOT NULL,
    attempts     integer NOT NULL DEFAULT 0,
    max_attempts integer NOT NULL DEFAULT 3,
    locked_by    text,
    locked_at    timestamptz,
    last_error   text
);

CREATE INDEX jobs_ready ON jobs (queue, priority DESC, run_at) WHERE status = 'ready';

INSERT INTO jobs (id, queue, payload, status, priority, run_at, attempts, max_attempts, locked_by, locked_at, last_error) VALUES
    (1, 'emails', '{"to": "ada@example.com"}', 'done', 0, '2026-11-15 09:00+00', 1, 3, NULL, NULL, NULL),
    (2, 'emails', '{"to": "grace@example.com"}', 'ready', 0, '2026-11-15 11:00+00', 0, 3, NULL, NULL, NULL),
    (3, 'emails', '{"to": "linus@example.com"}', 'ready', 5, '2026-11-15 11:30+00', 0, 3, NULL, NULL, NULL),
    (4, 'emails', '{"to": "alan@example.com"}', 'ready', 5, '2026-11-15 11:45+00', 0, 3, NULL, NULL, NULL),
    (5, 'emails', '{"to": "barbara@example.com"}', 'ready', 9, '2026-11-15 13:00+00', 0, 3, NULL, NULL, NULL),
    (6, 'reports', '{"month": "2026-10"}', 'ready', 9, '2026-11-15 10:00+00', 0, 3, NULL, NULL, NULL),
    (7, 'emails', '{"to": "dennis@example.com"}', 'running', 0, '2026-11-15 11:50+00', 1, 3, 'worker-2', '2026-11-15 11:58+00', NULL),
    (8, 'emails', '{"to": "ken@example.com"}', 'running', 0, '2026-11-15 11:00+00', 3, 3, 'worker-3', '2026-11-15 11:59+00', 'smtp timeout'),
    (9, 'reports', '{"month": "2026-09"}', 'running', 0, '2026-11-15 10:30+00', 1, 3, 'worker-1', '2026-11-15 11:40+00', NULL),
    (10, 'reports', '{"month": "2026-08"}', 'dead', 0, '2026-11-14 08:00+00', 3, 3, NULL, NULL, 'out of memory');
