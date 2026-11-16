ALTER TABLE orders
    ADD COLUMN status text NOT NULL DEFAULT 'placed'
        CONSTRAINT orders_status_check CHECK (status IN ('placed', 'shipped', 'delivered', 'cancelled'));

CREATE INDEX orders_page_idx ON orders (customer_id, placed_at DESC, id DESC);
