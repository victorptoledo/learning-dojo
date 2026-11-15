WITH per_event AS (
    SELECT e.id, e.title, v.name AS venue, v.capacity, count(t.id) AS sold
    FROM events AS e
    JOIN venues AS v ON v.id = e.venue_id
    LEFT JOIN tickets AS t ON t.event_id = e.id
    GROUP BY e.id, e.title, v.name, v.capacity
)
SELECT title,
       venue,
       sold,
       capacity - sold AS seats_left,
       dense_rank() OVER (PARTITION BY venue ORDER BY sold DESC) AS rank_in_venue
FROM per_event
ORDER BY venue, rank_in_venue, title;
