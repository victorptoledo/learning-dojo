SELECT m.name,
       count(l.id) AS loans,
       count(l.id) FILTER (WHERE l.returned_on IS NULL) AS open_loans
FROM members AS m
LEFT JOIN loans AS l ON l.member_id = m.id
GROUP BY m.id, m.name
ORDER BY loans DESC, m.name;
