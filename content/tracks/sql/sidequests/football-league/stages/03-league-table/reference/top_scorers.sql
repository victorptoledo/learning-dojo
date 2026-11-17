-- Own goals count for the other team, never for the scorer. WITH TIES compares the whole
-- ORDER BY, so the cut is made on goals alone and the names are sorted afterwards.
SELECT player, team, goals
FROM (
    SELECT p.name AS player, t.name AS team, count(*) AS goals
    FROM goals g
    JOIN players p ON p.id = g.player_id
    JOIN teams t ON t.id = p.team_id
    WHERE NOT g.own_goal
    GROUP BY p.id, p.name, t.name
    ORDER BY goals DESC
    FETCH FIRST 5 ROWS WITH TIES
) AS top
ORDER BY goals DESC, player;
