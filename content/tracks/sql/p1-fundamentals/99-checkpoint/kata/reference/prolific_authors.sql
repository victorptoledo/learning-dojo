SELECT a.name,
       count(DISTINCT b.id) AS books,
       count(l.id) AS loans
FROM authors AS a
JOIN books AS b ON b.author_id = a.id
LEFT JOIN loans AS l ON l.book_id = b.id
GROUP BY a.id, a.name
HAVING count(DISTINCT b.id) >= 2
ORDER BY loans DESC, a.name;
