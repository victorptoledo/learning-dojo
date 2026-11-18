-- Month-end interest: one entry per customer account, committed in batches, and recorded in
-- interest_runs in the same transaction as its postings, so a rerun skips what is done.

CREATE TABLE interest_runs (
    month      date NOT NULL,
    account_id bigint NOT NULL REFERENCES accounts,
    journal_id bigint REFERENCES journal,
    PRIMARY KEY (month, account_id)
);

CREATE PROCEDURE apply_interest(p_month date, p_rate numeric, p_batch integer DEFAULT 100)
LANGUAGE plpgsql AS $$
DECLARE
    month_start date := date_trunc('month', p_month)::date;
    account record;
    bank bigint;
    interest numeric;
    entry bigint;
    done integer := 0;
BEGIN
    FOR account IN
        SELECT a.id, a.currency FROM accounts a
        WHERE a.kind = 'customer'
          AND NOT EXISTS (SELECT FROM interest_runs r WHERE r.month = month_start AND r.account_id = a.id)
        ORDER BY a.id
    LOOP
        interest := round(balance(account.id) * p_rate, 2);
        entry := NULL;
        IF interest > 0 THEN
            SELECT id INTO bank FROM accounts
            WHERE kind = 'system' AND currency = account.currency ORDER BY id LIMIT 1;
            entry := post('interest ' || to_char(month_start, 'YYYY-MM'), jsonb_build_array(
                jsonb_build_object('account', bank, 'amount', -interest),
                jsonb_build_object('account', account.id, 'amount', interest)));
        END IF;
        INSERT INTO interest_runs (month, account_id, journal_id) VALUES (month_start, account.id, entry);
        done := done + 1;
        IF done % p_batch = 0 THEN
            COMMIT;
        END IF;
    END LOOP;
END
$$;
