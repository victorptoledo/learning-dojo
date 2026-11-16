-- updated_at is set by the database, whoever changes the row.
CREATE FUNCTION tickets_touch() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END
$$;

CREATE TRIGGER tickets_touch BEFORE UPDATE ON tickets
FOR EACH ROW EXECUTE FUNCTION tickets_touch();

-- One audit row per real change: the changed columns with their old and new values, or the
-- whole deleted row. updated_at and the generated doc are noise, not changes.
CREATE FUNCTION tickets_audit() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    v_changes jsonb;
BEGIN
    IF TG_OP = 'DELETE' THEN
        INSERT INTO audit_log (op, ticket_id, changes)
        VALUES ('DELETE', OLD.id, jsonb_build_object('deleted', to_jsonb(OLD) - 'doc'));
        RETURN NULL;
    END IF;

    SELECT coalesce(jsonb_object_agg(n.key, jsonb_build_object('old', o.value, 'new', n.value)), '{}')
    INTO v_changes
    FROM jsonb_each(to_jsonb(NEW) - 'updated_at' - 'doc') AS n
    JOIN jsonb_each(to_jsonb(OLD)) AS o ON o.key = n.key
    WHERE n.value IS DISTINCT FROM o.value;

    IF v_changes <> '{}' THEN
        INSERT INTO audit_log (op, ticket_id, changes) VALUES ('UPDATE', NEW.id, v_changes);
    END IF;
    RETURN NULL;
END
$$;

CREATE TRIGGER tickets_audit AFTER UPDATE OR DELETE ON tickets
FOR EACH ROW EXECUTE FUNCTION tickets_audit();
