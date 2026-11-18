SELECT id, title, price
FROM properties
WHERE features @> ARRAY['pool', 'sea-view']
  AND attrs @> '{"pets": true}'
ORDER BY price, id;
