-- One logical table, one partition per month. The primary key must include the partition key.
CREATE TABLE events (
    id        bigint GENERATED ALWAYS AS IDENTITY,
    tenant_id integer NOT NULL REFERENCES tenants (id),
    at        timestamptz NOT NULL,
    kind      text NOT NULL,
    payload   jsonb NOT NULL DEFAULT '{}',
    PRIMARY KEY (id, at)
) PARTITION BY RANGE (at);

CREATE TABLE events_2026_10 PARTITION OF events FOR VALUES FROM ('2026-10-01 00:00+00') TO ('2026-11-01 00:00+00');
CREATE TABLE events_2026_11 PARTITION OF events FOR VALUES FROM ('2026-11-01 00:00+00') TO ('2026-12-01 00:00+00');
CREATE TABLE events_2026_12 PARTITION OF events FOR VALUES FROM ('2026-12-01 00:00+00') TO ('2027-01-01 00:00+00');
CREATE TABLE events_default PARTITION OF events DEFAULT;

-- Created on the parent, so every partition (present and future) gets one.
CREATE INDEX events_tenant_at ON events (tenant_id, at);
