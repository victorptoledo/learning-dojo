-- A job queue that workers share: claims skip the jobs other workers are taking, leases expire,
-- failed jobs are retried with exponential backoff until they are dead, a job with a key is
-- enqueued once, and a group runs one job at a time.

CREATE TYPE job_status AS ENUM ('pending', 'running', 'done', 'dead');

CREATE TABLE jobs (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    queue        text NOT NULL,
    payload      jsonb NOT NULL DEFAULT '{}',
    priority     integer NOT NULL DEFAULT 0,
    run_at       timestamptz NOT NULL DEFAULT now(),
    status       job_status NOT NULL DEFAULT 'pending',
    attempts     integer NOT NULL DEFAULT 0,
    max_attempts integer NOT NULL DEFAULT 5,
    locked_by    text,
    locked_until timestamptz,
    last_error   text,
    dedupe_key   text,
    group_key    text
);

-- Only the jobs still to run or running are indexed, in the order a claim takes them: the
-- done and dead ones, most of the table, never get in the way.
CREATE INDEX jobs_next ON jobs (queue, priority DESC, run_at, id) WHERE status IN ('pending', 'running');

-- At most one live job per queue and key; done and dead jobs leave the key free again.
CREATE UNIQUE INDEX jobs_live_key ON jobs (queue, dedupe_key) WHERE status IN ('pending', 'running');

CREATE FUNCTION enqueue(p_queue text, p_payload jsonb, p_priority integer DEFAULT 0,
                        p_run_at timestamptz DEFAULT now()) RETURNS bigint
LANGUAGE sql AS $$
    INSERT INTO jobs (queue, payload, priority, run_at)
    VALUES (p_queue, p_payload, p_priority, p_run_at)
    RETURNING id
$$;

-- The insert waits for a transaction that is inserting the same key, then either inserts or
-- finds the conflict; in the second case the next statement sees the committed job.
CREATE FUNCTION enqueue_once(p_queue text, p_key text, p_payload jsonb) RETURNS bigint
LANGUAGE plpgsql AS $$
DECLARE
    got bigint;
BEGIN
    INSERT INTO jobs (queue, payload, dedupe_key)
    VALUES (p_queue, p_payload, p_key)
    ON CONFLICT (queue, dedupe_key) WHERE status IN ('pending', 'running') DO NOTHING
    RETURNING id INTO got;
    IF got IS NULL THEN
        SELECT id INTO got FROM jobs
        WHERE queue = p_queue AND dedupe_key = p_key AND status IN ('pending', 'running');
    END IF;
    RETURN got;
END
$$;

CREATE FUNCTION enqueue_grouped(p_queue text, p_group text, p_payload jsonb) RETURNS bigint
LANGUAGE sql AS $$
    INSERT INTO jobs (queue, payload, group_key)
    VALUES (p_queue, p_payload, p_group)
    RETURNING id
$$;

-- The candidates are read in order without locking; each one is taken only if its group is
-- free (no other transaction claiming in it right now, no job of it running on a live lease)
-- and if FOR UPDATE SKIP LOCKED can lock it. Nothing here ever waits.
CREATE FUNCTION claim(p_queue text, p_worker text) RETURNS bigint
LANGUAGE plpgsql AS $$
DECLARE
    cand record;
    got bigint;
BEGIN
    -- Expired after their last attempt: dead. Rows another worker is taking are skipped.
    UPDATE jobs SET status = 'dead', locked_by = NULL, locked_until = NULL
    WHERE id IN (SELECT id FROM jobs
                 WHERE queue = p_queue AND status = 'running'
                   AND locked_until < now() AND attempts >= max_attempts
                 FOR UPDATE SKIP LOCKED);

    FOR cand IN
        SELECT id, group_key FROM jobs
        WHERE queue = p_queue
          AND ((status = 'pending' AND run_at <= now())
               OR (status = 'running' AND locked_until < now() AND attempts < max_attempts))
        ORDER BY priority DESC, run_at, id
    LOOP
        IF cand.group_key IS NOT NULL THEN
            -- Held until this transaction ends: a concurrent claim skips the whole group.
            CONTINUE WHEN NOT pg_try_advisory_xact_lock(hashtext(p_queue || ':' || cand.group_key));
            CONTINUE WHEN EXISTS (SELECT FROM jobs
                                  WHERE queue = p_queue AND group_key = cand.group_key AND id <> cand.id
                                    AND status = 'running' AND locked_until >= now());
        END IF;
        UPDATE jobs
        SET status = 'running', locked_by = p_worker,
            locked_until = now() + interval '30 seconds', attempts = attempts + 1
        WHERE id = (SELECT id FROM jobs
                    WHERE id = cand.id
                      AND ((status = 'pending' AND run_at <= now())
                           OR (status = 'running' AND locked_until < now() AND attempts < max_attempts))
                    FOR UPDATE SKIP LOCKED)
        RETURNING id INTO got;
        IF got IS NOT NULL THEN
            RETURN got;
        END IF;
    END LOOP;
    RETURN NULL;
END
$$;

CREATE FUNCTION complete(p_job bigint, p_worker text) RETURNS boolean
LANGUAGE plpgsql AS $$
BEGIN
    UPDATE jobs SET status = 'done', locked_by = NULL, locked_until = NULL
    WHERE id = p_job AND status = 'running' AND locked_by = p_worker;
    RETURN FOUND;
END
$$;

CREATE FUNCTION heartbeat(p_job bigint, p_worker text) RETURNS boolean
LANGUAGE plpgsql AS $$
BEGIN
    UPDATE jobs SET locked_until = now() + interval '30 seconds'
    WHERE id = p_job AND status = 'running' AND locked_by = p_worker;
    RETURN FOUND;
END
$$;

CREATE FUNCTION fail(p_job bigint, p_worker text, p_error text) RETURNS job_status
LANGUAGE sql AS $$
    UPDATE jobs
    SET status = CASE WHEN attempts < max_attempts THEN 'pending' ELSE 'dead' END::job_status,
        run_at = CASE WHEN attempts < max_attempts
                      THEN now() + interval '10 seconds' * 2 ^ (attempts - 1)
                      ELSE run_at END,
        last_error = p_error, locked_by = NULL, locked_until = NULL
    WHERE id = p_job AND status = 'running' AND locked_by = p_worker
    RETURNING status
$$;
