SELECT id, title, round(word_similarity('Ribetta', title)::numeric, 3) AS similarity
FROM properties
WHERE 'Ribetta' <% title
ORDER BY word_similarity('Ribetta', title) DESC, id
LIMIT 15;
