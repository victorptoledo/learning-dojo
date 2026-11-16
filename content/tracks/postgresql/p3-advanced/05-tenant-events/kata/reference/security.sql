-- The API's role owns nothing: it can read and add events, never change or delete them.
CREATE ROLE app_api NOLOGIN;
GRANT USAGE ON SCHEMA public TO app_api;
GRANT SELECT ON tenants TO app_api;
GRANT SELECT, INSERT ON events TO app_api;

-- Each request runs SET LOCAL app.tenant_id = '<id>'. A setting that was set once in a session
-- reads as '' afterwards, and one never set does not exist: both mean "no tenant", no rows.
ALTER TABLE events ENABLE ROW LEVEL SECURITY;
ALTER TABLE events FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON events
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::integer)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::integer);
