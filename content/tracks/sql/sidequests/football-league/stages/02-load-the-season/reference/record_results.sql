-- Results arrive by round and team names; the match is found through both teams.
UPDATE matches m
SET home_goals = split_part(r.score, '-', 1)::integer,
    away_goals = split_part(r.score, '-', 2)::integer
FROM import_results r
JOIN teams h ON h.name = trim(r.home)
JOIN teams a ON a.name = trim(r.away)
WHERE m.home_team_id = h.id
  AND m.away_team_id = a.id
  AND m.round = r.round::integer;
