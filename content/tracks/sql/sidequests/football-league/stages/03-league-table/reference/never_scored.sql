-- An own goal is not a goal of one's own: only goals with own_goal = false count.
SELECT t.name AS team, p.name AS player, p.shirt_number
FROM players p
JOIN teams t ON t.id = p.team_id
WHERE NOT EXISTS (
    SELECT FROM goals g WHERE g.player_id = p.id AND NOT g.own_goal
)
ORDER BY team, p.shirt_number;
