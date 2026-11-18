SELECT id, city, price
FROM properties
WHERE attrs @> '{"energy": "A", "floor": 20}'
ORDER BY id;
