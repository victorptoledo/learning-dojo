-- Price history: one price per product and currency at any moment, as [start, end) ranges.

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE prices (
    product_id bigint NOT NULL REFERENCES products,
    currency   currency NOT NULL,
    amount     numeric(10, 2) NOT NULL CHECK (amount > 0),
    valid      tstzrange NOT NULL
        CHECK (NOT isempty(valid) AND NOT lower_inf(valid) AND lower_inc(valid) AND NOT upper_inc(valid)),
    EXCLUDE USING gist (product_id WITH =, currency WITH =, valid WITH &&)
);

CREATE FUNCTION price_at(p_product bigint, p_currency currency, p_at timestamptz) RETURNS numeric
LANGUAGE sql STABLE AS $$
    SELECT amount FROM prices
    WHERE product_id = p_product AND currency = p_currency AND valid @> p_at
$$;

CREATE FUNCTION set_price(p_product bigint, p_currency currency, p_amount numeric, p_from timestamptz)
RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (SELECT FROM prices
               WHERE product_id = p_product AND currency = p_currency AND lower(valid) > p_from) THEN
        RAISE EXCEPTION 'product % already has a % price starting after %', p_product, p_currency, p_from
            USING ERRCODE = 'invalid_parameter_value';
    END IF;
    DELETE FROM prices
    WHERE product_id = p_product AND currency = p_currency AND lower(valid) = p_from;
    UPDATE prices SET valid = tstzrange(lower(valid), p_from)
    WHERE product_id = p_product AND currency = p_currency AND valid @> p_from;
    INSERT INTO prices (product_id, currency, amount, valid)
    VALUES (p_product, p_currency, p_amount, tstzrange(p_from, NULL));
END
$$;

-- What p_qty units cost at p_at, with the best discount that applies.
CREATE FUNCTION final_price(p_product bigint, p_currency currency, p_qty integer, p_at timestamptz)
RETURNS numeric
LANGUAGE plpgsql STABLE AS $$
DECLARE
    unit numeric := price_at(p_product, p_currency, p_at);
    best integer;
BEGIN
    IF p_qty IS NULL OR p_qty < 1 THEN
        RAISE EXCEPTION 'quantity must be at least 1, got %', p_qty
            USING ERRCODE = 'invalid_parameter_value';
    END IF;
    IF unit IS NULL THEN
        RETURN NULL;
    END IF;
    SELECT max((rule ->> 'value')::integer) INTO best
    FROM discounts
    WHERE product_id = p_product
      AND (kind = 'percent' OR (kind = 'bulk' AND p_qty >= (rule ->> 'min_qty')::integer));
    RETURN round(unit * p_qty * (100 - coalesce(best, 0)) / 100, 2);
END
$$;
