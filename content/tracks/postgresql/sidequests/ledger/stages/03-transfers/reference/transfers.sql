-- Transfers: both accounts are locked, in id order, before the balance is read, so two
-- transfers from one account take turns and a customer never goes below zero.

CREATE FUNCTION transfer(p_from bigint, p_to bigint, p_amount numeric, p_memo text) RETURNS bigint
LANGUAGE plpgsql AS $$
DECLARE
    src accounts;
    dst accounts;
BEGIN
    IF p_amount IS NULL OR p_amount <= 0 THEN
        RAISE EXCEPTION 'a transfer needs an amount above zero, got %', p_amount
            USING ERRCODE = 'invalid_parameter_value';
    END IF;
    IF p_from = p_to THEN
        RAISE EXCEPTION 'a transfer needs two different accounts'
            USING ERRCODE = 'invalid_parameter_value';
    END IF;
    -- NO KEY UPDATE: it does not block the foreign key checks of other postings.
    PERFORM FROM accounts WHERE id IN (p_from, p_to) ORDER BY id FOR NO KEY UPDATE;
    SELECT * INTO src FROM accounts WHERE id = p_from;
    SELECT * INTO dst FROM accounts WHERE id = p_to;
    IF src.id IS NULL OR dst.id IS NULL THEN
        RAISE EXCEPTION 'account % or % does not exist', p_from, p_to
            USING ERRCODE = 'invalid_parameter_value';
    END IF;
    IF src.currency <> dst.currency THEN
        RAISE EXCEPTION 'account % is in % and account % in %', p_from, src.currency, p_to, dst.currency
            USING ERRCODE = 'invalid_parameter_value';
    END IF;
    IF src.kind = 'customer' AND balance(p_from) < p_amount THEN
        RAISE EXCEPTION 'insufficient funds in account %', p_from
            USING ERRCODE = 'LD001';
    END IF;
    RETURN post(p_memo, jsonb_build_array(
        jsonb_build_object('account', p_from, 'amount', -p_amount),
        jsonb_build_object('account', p_to, 'amount', p_amount)));
END
$$;
