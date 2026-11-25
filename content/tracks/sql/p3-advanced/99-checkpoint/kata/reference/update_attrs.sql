-- Optimistic concurrency: the version read by the admin is part of the WHERE, so a save made
-- in between turns this into "0 rows". || merges keys into the document and - removes one,
-- leaving every other key as it was.
UPDATE products
SET attrs = (attrs || '{"color": "blue"}') - 'promo',
    version = version + 1
WHERE id = 42
  AND version = 3
RETURNING id, version;
