-- Equality on customer_id first, then the sort column: the index returns the orders of one
-- customer already ordered by time, so LIMIT 10 reads just 10 entries.
CREATE INDEX orders_customer_placed ON orders (customer_id, placed_at);

-- The query compares lower(email), so the index must store lower(email).
CREATE INDEX customers_email_lower ON customers (lower(email));

-- Only the 1% of pending orders, already sorted by time.
CREATE INDEX orders_pending_placed ON orders (placed_at) WHERE status = 'pending';
