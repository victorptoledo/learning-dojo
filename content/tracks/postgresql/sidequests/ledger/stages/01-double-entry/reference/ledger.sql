-- Double-entry ledger: an entry's postings must balance, checked when the transaction commits.

CREATE TABLE journal (
    id        bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    memo      text NOT NULL,
    posted_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE postings (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    journal_id bigint NOT NULL REFERENCES journal,
    account_id bigint NOT NULL REFERENCES accounts,
    amount     numeric(14, 2) NOT NULL CHECK (amount <> 0)
);

CREATE INDEX postings_journal ON postings (journal_id);
CREATE INDEX postings_account ON postings (account_id);

-- Raises check_violation unless the entry has two postings or more, in one currency,
-- adding up to zero.
CREATE FUNCTION check_entry(p_journal bigint) RETURNS void
LANGUAGE plpgsql AS $$
DECLARE
    postings_count integer;
    total numeric;
    currencies integer;
BEGIN
    SELECT count(*), coalesce(sum(p.amount), 0), count(DISTINCT a.currency)
    INTO postings_count, total, currencies
    FROM postings p JOIN accounts a ON a.id = p.account_id
    WHERE p.journal_id = p_journal;
    IF postings_count < 2 THEN
        RAISE EXCEPTION 'journal entry % has % posting(s), it needs at least 2', p_journal, postings_count
            USING ERRCODE = 'check_violation';
    ELSIF currencies > 1 THEN
        RAISE EXCEPTION 'journal entry % mixes currencies', p_journal
            USING ERRCODE = 'check_violation';
    ELSIF total <> 0 THEN
        RAISE EXCEPTION 'journal entry % does not balance: its postings add up to %', p_journal, total
            USING ERRCODE = 'check_violation';
    END IF;
END
$$;

CREATE FUNCTION check_entry_trigger() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF TG_TABLE_NAME = 'journal' THEN
        PERFORM check_entry(NEW.id);
    ELSE
        PERFORM check_entry(NEW.journal_id);
    END IF;
    RETURN NULL;
END
$$;

-- Deferred: they run at COMMIT (or SET CONSTRAINTS ... IMMEDIATE), when the entry is complete.
CREATE CONSTRAINT TRIGGER postings_balance
    AFTER INSERT OR UPDATE ON postings
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION check_entry_trigger();

CREATE CONSTRAINT TRIGGER journal_has_postings
    AFTER INSERT ON journal
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION check_entry_trigger();

CREATE FUNCTION post(p_memo text, p_postings jsonb) RETURNS bigint
LANGUAGE plpgsql AS $$
DECLARE
    entry bigint;
BEGIN
    INSERT INTO journal (memo) VALUES (p_memo) RETURNING id INTO entry;
    INSERT INTO postings (journal_id, account_id, amount)
    SELECT entry, (p ->> 'account')::bigint, (p ->> 'amount')::numeric
    FROM jsonb_array_elements(p_postings) AS p;
    -- Fail now, inside post, rather than at the caller's COMMIT.
    PERFORM check_entry(entry);
    RETURN entry;
END
$$;

CREATE FUNCTION balance(p_account bigint) RETURNS numeric
LANGUAGE sql STABLE AS $$
    SELECT coalesce(sum(amount), 0) FROM postings WHERE account_id = p_account
$$;
