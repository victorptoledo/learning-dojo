-- Stage 1 contract: schema.sql creates teams, players, matches and goals with their rules.
-- Tests never give ids: they look rows up by name.

SELECT dojo.script_ok(
    'the four tables take rows and generate the ids',
    'schema.sql',
    $$SELECT (SELECT count(*) = 2 AND count(DISTINCT id) = 2 FROM teams)
      AND (SELECT count(*) = 1 FROM players WHERE id IS NOT NULL)
      AND (SELECT count(*) = 1 FROM matches WHERE id IS NOT NULL)
      AND (SELECT own_goal = false FROM goals WHERE minute = 17)$$,
    'two teams, a player, a match and a goal (without own_goal) should insert, with generated ids and own_goal false',
    after => $$INSERT INTO teams (name, city) VALUES ('Red', 'Rome'), ('Blue', 'Bern');
               INSERT INTO players (team_id, name, shirt_number)
                   SELECT id, 'Ann', 9 FROM teams WHERE name = 'Red';
               INSERT INTO matches (round, played_on, home_team_id, away_team_id, home_goals, away_goals)
                   VALUES (1, '2026-08-01', (SELECT id FROM teams WHERE name = 'Red'),
                           (SELECT id FROM teams WHERE name = 'Blue'), 1, 0);
               INSERT INTO goals (match_id, player_id, minute)
                   SELECT m.id, p.id, 17 FROM matches m, players p$$
);

SELECT dojo.script_fails(
    'team names are unique',
    'schema.sql',
    $$INSERT INTO teams (name, city) VALUES ('Red', 'Riga')$$,
    '23505',
    'two teams cannot have the same name',
    after => $$INSERT INTO teams (name, city) VALUES ('Red', 'Rome')$$
);

SELECT dojo.script_fails(
    'a team needs a name',
    'schema.sql',
    $$INSERT INTO teams (city) VALUES ('Rome')$$,
    '23502',
    'teams.name is required'
);

SELECT dojo.script_fails(
    'a team needs a city',
    'schema.sql',
    $$INSERT INTO teams (name) VALUES ('Red')$$,
    '23502',
    'teams.city is required'
);

SELECT dojo.script_fails(
    'a player belongs to an existing team',
    'schema.sql',
    $$INSERT INTO players (team_id, name, shirt_number) VALUES (424242, 'Ann', 9)$$,
    '23503',
    'players.team_id must reference teams'
);

SELECT dojo.script_fails(
    'a player needs a team',
    'schema.sql',
    $$INSERT INTO players (name, shirt_number) VALUES ('Ann', 9)$$,
    '23502',
    'players.team_id is required'
);

SELECT dojo.script_fails(
    'shirt numbers start at 1',
    'schema.sql',
    $$INSERT INTO players (team_id, name, shirt_number) SELECT id, 'Ann', 0 FROM teams$$,
    '23514',
    'shirt_number must be between 1 and 99',
    after => $$INSERT INTO teams (name, city) VALUES ('Red', 'Rome')$$
);

SELECT dojo.script_fails(
    'shirt numbers stop at 99',
    'schema.sql',
    $$INSERT INTO players (team_id, name, shirt_number) SELECT id, 'Ann', 100 FROM teams$$,
    '23514',
    'shirt_number must be between 1 and 99',
    after => $$INSERT INTO teams (name, city) VALUES ('Red', 'Rome')$$
);

SELECT dojo.script_fails(
    'a shirt number is used once per team',
    'schema.sql',
    $$INSERT INTO players (team_id, name, shirt_number) SELECT id, 'Bea', 9 FROM teams$$,
    '23505',
    'two players of the same team cannot wear the same number',
    after => $$INSERT INTO teams (name, city) VALUES ('Red', 'Rome');
               INSERT INTO players (team_id, name, shirt_number) SELECT id, 'Ann', 9 FROM teams$$
);

SELECT dojo.script_fails(
    'a player name is used once per team',
    'schema.sql',
    $$INSERT INTO players (team_id, name, shirt_number) SELECT id, 'Ann', 10 FROM teams$$,
    '23505',
    'a team cannot list the same player twice',
    after => $$INSERT INTO teams (name, city) VALUES ('Red', 'Rome');
               INSERT INTO players (team_id, name, shirt_number) SELECT id, 'Ann', 9 FROM teams$$
);

SELECT dojo.script_ok(
    'two teams may use the same shirt number and name',
    'schema.sql',
    $$(SELECT count(*) = 2 FROM players WHERE shirt_number = 9 AND name = 'Ann')$$,
    'Ann wearing 9 in two different teams should be accepted',
    after => $$INSERT INTO teams (name, city) VALUES ('Red', 'Rome'), ('Blue', 'Bern');
               INSERT INTO players (team_id, name, shirt_number) SELECT id, 'Ann', 9 FROM teams$$
);

SELECT dojo.script_fails(
    'a team never plays itself',
    'schema.sql',
    $$INSERT INTO matches (round, played_on, home_team_id, away_team_id)
          SELECT 1, '2026-08-01', id, id FROM teams$$,
    '23514',
    'home_team_id and away_team_id must differ',
    after => $$INSERT INTO teams (name, city) VALUES ('Red', 'Rome')$$
);

SELECT dojo.script_fails(
    'rounds start at 1',
    'schema.sql',
    $$INSERT INTO matches (round, played_on, home_team_id, away_team_id)
          VALUES (0, '2026-08-01', (SELECT id FROM teams WHERE name = 'Red'),
                  (SELECT id FROM teams WHERE name = 'Blue'))$$,
    '23514',
    'round must be at least 1',
    after => $$INSERT INTO teams (name, city) VALUES ('Red', 'Rome'), ('Blue', 'Bern')$$
);

SELECT dojo.script_fails(
    'a match needs a date',
    'schema.sql',
    $$INSERT INTO matches (round, home_team_id, away_team_id)
          VALUES (1, (SELECT id FROM teams WHERE name = 'Red'), (SELECT id FROM teams WHERE name = 'Blue'))$$,
    '23502',
    'played_on is required',
    after => $$INSERT INTO teams (name, city) VALUES ('Red', 'Rome'), ('Blue', 'Bern')$$
);

SELECT dojo.script_fails(
    'a match references existing teams',
    'schema.sql',
    $$INSERT INTO matches (round, played_on, home_team_id, away_team_id)
          VALUES (1, '2026-08-01', (SELECT id FROM teams WHERE name = 'Red'), 424242)$$,
    '23503',
    'away_team_id must reference teams',
    after => $$INSERT INTO teams (name, city) VALUES ('Red', 'Rome')$$
);

SELECT dojo.script_ok(
    'a match not played yet has no score',
    'schema.sql',
    $$(SELECT home_goals IS NULL AND away_goals IS NULL FROM matches)$$,
    'a match inserted without goals should be accepted, with both scores NULL',
    after => $$INSERT INTO teams (name, city) VALUES ('Red', 'Rome'), ('Blue', 'Bern');
               INSERT INTO matches (round, played_on, home_team_id, away_team_id)
                   VALUES (10, '2026-10-22', (SELECT id FROM teams WHERE name = 'Red'),
                           (SELECT id FROM teams WHERE name = 'Blue'))$$
);

SELECT dojo.script_fails(
    'a score has both sides',
    'schema.sql',
    $$INSERT INTO matches (round, played_on, home_team_id, away_team_id, home_goals)
          VALUES (1, '2026-08-01', (SELECT id FROM teams WHERE name = 'Red'),
                  (SELECT id FROM teams WHERE name = 'Blue'), 2)$$,
    '23514',
    'home_goals and away_goals are both NULL or both set',
    after => $$INSERT INTO teams (name, city) VALUES ('Red', 'Rome'), ('Blue', 'Bern')$$
);

SELECT dojo.script_fails(
    'goals are never negative',
    'schema.sql',
    $$INSERT INTO matches (round, played_on, home_team_id, away_team_id, home_goals, away_goals)
          VALUES (1, '2026-08-01', (SELECT id FROM teams WHERE name = 'Red'),
                  (SELECT id FROM teams WHERE name = 'Blue'), 1, -1)$$,
    '23514',
    'home_goals and away_goals cannot be negative',
    after => $$INSERT INTO teams (name, city) VALUES ('Red', 'Rome'), ('Blue', 'Bern')$$
);

SELECT dojo.script_fails(
    'a club hosts the same opponent once',
    'schema.sql',
    $$INSERT INTO matches (round, played_on, home_team_id, away_team_id)
          VALUES (7, '2026-09-15', (SELECT id FROM teams WHERE name = 'Red'),
                  (SELECT id FROM teams WHERE name = 'Blue'))$$,
    '23505',
    'the pair (home_team_id, away_team_id) is unique',
    after => $$INSERT INTO teams (name, city) VALUES ('Red', 'Rome'), ('Blue', 'Bern');
               INSERT INTO matches (round, played_on, home_team_id, away_team_id)
                   VALUES (1, '2026-08-01', (SELECT id FROM teams WHERE name = 'Red'),
                           (SELECT id FROM teams WHERE name = 'Blue'))$$
);

SELECT dojo.script_ok(
    'the return match is another pair',
    'schema.sql',
    $$(SELECT count(*) = 2 FROM matches)$$,
    'Red at home to Blue and Blue at home to Red should both be accepted',
    after => $$INSERT INTO teams (name, city) VALUES ('Red', 'Rome'), ('Blue', 'Bern');
               INSERT INTO matches (round, played_on, home_team_id, away_team_id)
                   SELECT r, '2026-08-01', h.id, a.id
                   FROM (VALUES (1, 'Red', 'Blue'), (6, 'Blue', 'Red')) AS v(r, home, away)
                   JOIN teams h ON h.name = v.home JOIN teams a ON a.name = v.away$$
);

SELECT dojo.script_fails(
    'a goal minute is between 1 and 120',
    'schema.sql',
    $$INSERT INTO goals (match_id, player_id, minute) SELECT m.id, p.id, 121 FROM matches m, players p$$,
    '23514',
    'minute must be between 1 and 120',
    after => $$INSERT INTO teams (name, city) VALUES ('Red', 'Rome'), ('Blue', 'Bern');
               INSERT INTO players (team_id, name, shirt_number) SELECT id, 'Ann', 9 FROM teams WHERE name = 'Red';
               INSERT INTO matches (round, played_on, home_team_id, away_team_id, home_goals, away_goals)
                   VALUES (1, '2026-08-01', (SELECT id FROM teams WHERE name = 'Red'),
                           (SELECT id FROM teams WHERE name = 'Blue'), 1, 0)$$
);

SELECT dojo.script_fails(
    'a goal is scored by an existing player',
    'schema.sql',
    $$INSERT INTO goals (match_id, player_id, minute) SELECT id, 424242, 10 FROM matches$$,
    '23503',
    'goals.player_id must reference players',
    after => $$INSERT INTO teams (name, city) VALUES ('Red', 'Rome'), ('Blue', 'Bern');
               INSERT INTO matches (round, played_on, home_team_id, away_team_id, home_goals, away_goals)
                   VALUES (1, '2026-08-01', (SELECT id FROM teams WHERE name = 'Red'),
                           (SELECT id FROM teams WHERE name = 'Blue'), 1, 0)$$
);

SELECT dojo.script_fails(
    'a goal belongs to a match',
    'schema.sql',
    $$INSERT INTO goals (player_id, minute) SELECT id, 10 FROM players$$,
    '23502',
    'goals.match_id is required',
    after => $$INSERT INTO teams (name, city) VALUES ('Red', 'Rome');
               INSERT INTO players (team_id, name, shirt_number) SELECT id, 'Ann', 9 FROM teams$$
);

SELECT dojo.script_ok(
    'deleting a match deletes its goals',
    'schema.sql',
    $$(SELECT count(*) = 0 FROM goals)$$,
    'after DELETE FROM matches, its goals should be gone (ON DELETE CASCADE)',
    after => $$INSERT INTO teams (name, city) VALUES ('Red', 'Rome'), ('Blue', 'Bern');
               INSERT INTO players (team_id, name, shirt_number) SELECT id, 'Ann', 9 FROM teams WHERE name = 'Red';
               INSERT INTO matches (round, played_on, home_team_id, away_team_id, home_goals, away_goals)
                   VALUES (1, '2026-08-01', (SELECT id FROM teams WHERE name = 'Red'),
                           (SELECT id FROM teams WHERE name = 'Blue'), 2, 0);
               INSERT INTO goals (match_id, player_id, minute) SELECT m.id, p.id, x
                   FROM matches m, players p, (VALUES (12), (80)) AS v(x);
               DELETE FROM matches$$
);

SELECT dojo.script_fails(
    'a team with players cannot be deleted',
    'schema.sql',
    $$DELETE FROM teams WHERE name = 'Red'$$,
    '23503',
    'deleting a team must not leave its players without a team',
    after => $$INSERT INTO teams (name, city) VALUES ('Red', 'Rome');
               INSERT INTO players (team_id, name, shirt_number) SELECT id, 'Ann', 9 FROM teams$$
);
