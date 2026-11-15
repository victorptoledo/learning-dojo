SELECT b.title
FROM books AS b
WHERE NOT EXISTS (
    SELECT 1
    FROM loans AS l
    WHERE l.book_id = b.id
      AND l.returned_on IS NULL
)
ORDER BY b.title;
