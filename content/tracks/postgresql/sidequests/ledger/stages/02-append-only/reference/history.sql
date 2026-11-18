-- The ledger is append-only: mistakes are fixed by reversing entries, never by editing them.

ALTER TABLE journal ADD COLUMN reverses bigint UNIQUE REFERENCES journal;

CREATE FUNCTION refuse_change() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'the ledger is append-only: % on % is not allowed', TG_OP, TG_TABLE_NAME
        USING ERRCODE = 'LD002', HINT = 'reverse the entry instead';
END
$$;

CREATE TRIGGER journal_append_only BEFORE UPDATE OR DELETE ON journal
    FOR EACH ROW EXECUTE FUNCTION refuse_change();
CREATE TRIGGER journal_no_truncate BEFORE TRUNCATE ON journal
    FOR EACH STATEMENT EXECUTE FUNCTION refuse_change();
CREATE TRIGGER postings_append_only BEFORE UPDATE OR DELETE ON postings
    FOR EACH ROW EXECUTE FUNCTION refuse_change();
CREATE TRIGGER postings_no_truncate BEFORE TRUNCATE ON postings
    FOR EACH STATEMENT EXECUTE FUNCTION refuse_change();

CREATE FUNCTION reverse_entry(p_journal bigint, p_memo text) RETURNS bigint
LANGUAGE plpgsql AS $$
DECLARE
    original journal;
    entry bigint;
BEGIN
    SELECT * INTO original FROM journal WHERE id = p_journal;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'journal entry % does not exist', p_journal
            USING ERRCODE = 'invalid_parameter_value';
    END IF;
    IF original.reverses IS NOT NULL THEN
        RAISE EXCEPTION 'journal entry % is a reversal; post a new entry instead', p_journal
            USING ERRCODE = 'invalid_parameter_value';
    END IF;
    -- The unique key on reverses refuses a second reversal, and makes a concurrent one wait.
    INSERT INTO journal (memo, reverses) VALUES (p_memo, p_journal) RETURNING id INTO entry;
    INSERT INTO postings (journal_id, account_id, amount)
    SELECT entry, account_id, -amount FROM postings WHERE journal_id = p_journal ORDER BY id;
    RETURN entry;
END
$$;
