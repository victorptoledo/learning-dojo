-- Every played match seen from both sides, then one GROUP BY per team.
WITH sides AS (
    SELECT home_team_id AS team_id, home_goals AS scored, away_goals AS conceded
    FROM matches
    WHERE home_goals IS NOT NULL
    UNION ALL
    SELECT away_team_id, away_goals, home_goals
    FROM matches
    WHERE home_goals IS NOT NULL
)
SELECT t.name AS team,
       count(s.team_id) AS played,
       count(*) FILTER (WHERE s.scored > s.conceded) AS won,
       count(*) FILTER (WHERE s.scored = s.conceded) AS drawn,
       count(*) FILTER (WHERE s.scored < s.conceded) AS lost,
       coalesce(sum(s.scored), 0) AS goals_for,
       coalesce(sum(s.conceded), 0) AS goals_against,
       coalesce(sum(s.scored - s.conceded), 0) AS goal_difference,
       coalesce(sum(CASE WHEN s.scored > s.conceded THEN 3
                         WHEN s.scored = s.conceded THEN 1
                         ELSE 0 END), 0) AS points
FROM teams t
LEFT JOIN sides s ON s.team_id = t.id
GROUP BY t.id, t.name
ORDER BY points DESC, goal_difference DESC, goals_for DESC, team;
