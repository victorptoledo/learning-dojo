SELECT m.name, b.title, DATE '2026-11-15' - l.due_on AS days_overdue
FROM loans AS l
JOIN members AS m ON m.id = l.member_id
JOIN books AS b ON b.id = l.book_id
WHERE l.returned_on IS NULL
  AND l.due_on < DATE '2026-11-15'
ORDER BY days_overdue DESC, m.name, b.title;
