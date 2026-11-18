SELECT count(*) AS listings
FROM properties
WHERE location <@ circle '((20, 80), 2)'
  AND bedrooms >= 3;
