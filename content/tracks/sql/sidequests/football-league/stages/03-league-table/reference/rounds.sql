-- Every round, also those not played yet: count() ignores the NULL scores and avg() of
-- nothing is NULL.
SELECT round,
       min(played_on) AS played_on,
       count(home_goals) AS played,
       coalesce(sum(home_goals + away_goals), 0) AS goals,
       round(avg(home_goals + away_goals), 2) AS average
FROM matches
GROUP BY round
ORDER BY round;
