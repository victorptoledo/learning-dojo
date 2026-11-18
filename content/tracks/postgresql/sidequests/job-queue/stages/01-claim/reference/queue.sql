-- A job queue that workers share: claims skip the jobs other workers are taking.

CREATE TYPE job_status AS ENUM ('pending', 'running', 'done', 'dead');

CREATE TABLE jobs (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    queue        text NOT NULL,
    payload      jsonb NOT NULL DEFAULT '{}',
    priority     integer NOT NULL DEFAULT 0,
    run_at       timestamptz NOT NULL DEFAULT now(),
    status       job_status NOT NULL DEFAULT 'pending',
    attempts     integer NOT NULL DEFAULT 0,
    locked_by    text,
    locked_until timestamptz
);

-- Only the jobs still to run are indexed, in the order a claim takes them: the done ones,
-- most of the table, never get in the way.
CREATE INDEX jobs_next ON jobs (queue, priority DESC, run_at, id) WHERE status = 'pending';

CREATE FUNCTION enqueue(p_queue text, p_payload jsonb, p_priority integer DEFAULT 0,
                        p_run_at timestamptz DEFAULT now()) RETURNS bigint
LANGUAGE sql AS $$
    INSERT INTO jobs (queue, payload, priority, run_at)
    VALUES (p_queue, p_payload, p_priority, p_run_at)
    RETURNING id
$$;

CREATE FUNCTION claim(p_queue text, p_worker text) RETURNS bigint
LANGUAGE sql AS $$
    UPDATE jobs
    SET status = 'running', locked_by = p_worker,
        locked_until = now() + interval '30 seconds', attempts = attempts + 1
    WHERE id = (SELECT id FROM jobs
                WHERE queue = p_queue AND status = 'pending' AND run_at <= now()
                ORDER BY priority DESC, run_at, id
                LIMIT 1
                FOR UPDATE SKIP LOCKED)
    RETURNING id
$$;

CREATE FUNCTION complete(p_job bigint, p_worker text) RETURNS boolean
LANGUAGE plpgsql AS $$
BEGIN
    UPDATE jobs SET status = 'done', locked_by = NULL, locked_until = NULL
    WHERE id = p_job AND status = 'running' AND locked_by = p_worker;
    RETURN FOUND;
END
$$;
