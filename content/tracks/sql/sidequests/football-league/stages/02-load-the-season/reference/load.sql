-- The season from the spreadsheet: trim the names, cast the text, and turn an empty score
-- into a match not played yet.

INSERT INTO teams (name, city)
SELECT trim(name), trim(city)
FROM import_teams;

INSERT INTO players (team_id, name, shirt_number)
SELECT t.id, trim(p.player), p.shirt::integer
FROM import_players p
JOIN teams t ON t.name = trim(p.team);

INSERT INTO matches (round, played_on, home_team_id, away_team_id, home_goals, away_goals)
SELECT m.round::integer,
       m.played_on::date,
       h.id,
       a.id,
       nullif(split_part(m.score, '-', 1), '')::integer,
       nullif(split_part(m.score, '-', 2), '')::integer
FROM import_matches m
JOIN teams h ON h.name = trim(m.home)
JOIN teams a ON a.name = trim(m.away);

INSERT INTO goals (match_id, player_id, minute, own_goal)
SELECT m.id, p.id, g.minute::integer, g.own_goal = 'yes'
FROM import_goals g
JOIN teams h ON h.name = trim(g.home)
JOIN teams a ON a.name = trim(g.away)
JOIN matches m ON m.home_team_id = h.id AND m.away_team_id = a.id
JOIN teams st ON st.name = trim(g.scorer_team)
JOIN players p ON p.team_id = st.id AND p.name = trim(g.scorer);
