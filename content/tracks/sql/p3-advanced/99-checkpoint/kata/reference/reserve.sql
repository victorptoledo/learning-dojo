-- One statement: the availability test is in the UPDATE's own WHERE. If another session holds
-- the row, the UPDATE waits, then re-reads the committed row and tests the WHERE again, so it
-- can never promise units that were sold in between.
UPDATE stock
SET reserved = reserved + 2
WHERE product_id = 42
  AND on_hand - reserved >= 2
RETURNING product_id, on_hand - reserved AS available;
