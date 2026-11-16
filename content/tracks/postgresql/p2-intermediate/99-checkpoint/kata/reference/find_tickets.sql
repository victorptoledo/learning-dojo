SELECT t.id, t.title
FROM tickets AS t,
     websearch_to_tsquery('english', 'login error') AS q
WHERE t.doc @@ q
  AND t.status = 'open'
  AND t.metadata @> '{"customer": {"plan": "pro"}}'
ORDER BY ts_rank(t.doc, q) DESC, t.id
LIMIT 5;
