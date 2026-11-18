SELECT id, round((location <-> point '(50.5, 50.5)')::numeric, 3) AS distance
FROM properties
ORDER BY location <-> point '(50.5, 50.5)'
LIMIT 10;
