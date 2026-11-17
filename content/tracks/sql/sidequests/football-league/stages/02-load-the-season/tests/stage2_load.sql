-- Stage 2 contract: load.sql fills the schema from the import_* tables of the fixture,
-- squad_changes.sql upserts players and record_results.sql writes the scores of round 9.

SELECT dojo.script_eq(
    'loads every team, player, match and goal',
    'load.sql',
    $$SELECT (SELECT count(*) FROM teams), (SELECT count(*) FROM players),
             (SELECT count(*) FROM matches), (SELECT count(*) FROM goals)$$,
    $$VALUES (6, 24, 30, 68)$$,
    setup => $$SELECT dojo.run_file('schema.sql')$$
);

SELECT dojo.script_eq(
    'team names arrive without the stray spaces',
    'load.sql',
    $$SELECT name, city FROM teams ORDER BY name$$,
    $$VALUES ('Ashford Athletic', 'Ashford'), ('Bramble United', 'Bramble'),
             ('Coldwater City', 'Coldwater'), ('Dunmore Rangers', 'Dunmore'),
             ('Eastgate Wanderers', 'Eastgate'), ('Fairhaven Town', 'Fairhaven')$$,
    ordered => true,
    setup => $$SELECT dojo.run_file('schema.sql')$$
);

SELECT dojo.script_eq(
    'each club plays ten matches, five at home',
    'load.sql',
    $$SELECT t.name, count(*) FILTER (WHERE m.home_team_id = t.id), count(*)
      FROM teams t JOIN matches m ON t.id IN (m.home_team_id, m.away_team_id)
      GROUP BY t.name$$,
    $$VALUES ('Ashford Athletic', 5, 10), ('Bramble United', 5, 10), ('Coldwater City', 5, 10),
             ('Dunmore Rangers', 5, 10), ('Eastgate Wanderers', 5, 10), ('Fairhaven Town', 5, 10)$$,
    setup => $$SELECT dojo.run_file('schema.sql')$$
);

SELECT dojo.script_eq(
    'a score of 2-1 becomes two numbers',
    'load.sql',
    $$SELECT m.round, m.played_on, m.home_goals, m.away_goals
      FROM matches m
      JOIN teams h ON h.id = m.home_team_id AND h.name = 'Coldwater City'
      JOIN teams a ON a.id = m.away_team_id AND a.name = 'Dunmore Rangers'$$,
    $$VALUES (1, date '2026-08-01', 2, 1)$$,
    setup => $$SELECT dojo.run_file('schema.sql')$$
);

SELECT dojo.script_eq(
    'an empty score is a match not played yet',
    'load.sql',
    $$SELECT round, count(*), count(home_goals), count(away_goals)
      FROM matches WHERE round >= 8 GROUP BY round$$,
    $$VALUES (8, 3, 3, 3), (9, 3, 0, 0), (10, 3, 0, 0)$$,
    setup => $$SELECT dojo.run_file('schema.sql')$$
);

SELECT dojo.script_eq(
    'players keep their team and shirt',
    'load.sql',
    $$SELECT t.name, p.name, p.shirt_number
      FROM players p JOIN teams t ON t.id = p.team_id
      WHERE t.name = 'Coldwater City'$$,
    $$VALUES ('Coldwater City', 'Ivan Petrov', 1), ('Coldwater City', 'Jack Silva', 4),
             ('Coldwater City', 'Kai Tanaka', 9), ('Coldwater City', 'Leo Walsh', 10)$$,
    setup => $$SELECT dojo.run_file('schema.sql')$$
);

SELECT dojo.script_eq(
    'goals land in the right match with their scorer',
    'load.sql',
    $$SELECT p.name, g.minute, g.own_goal
      FROM goals g
      JOIN players p ON p.id = g.player_id
      JOIN matches m ON m.id = g.match_id
      JOIN teams h ON h.id = m.home_team_id AND h.name = 'Ashford Athletic'
      JOIN teams a ON a.id = m.away_team_id AND a.name = 'Fairhaven Town'
      ORDER BY g.minute$$,
    $$VALUES ('Caio Reyes', 45, false), ('Caio Reyes', 68, false),
             ('Vic Ito', 85, false), ('Vic Ito', 86, true)$$,
    ordered => true,
    setup => $$SELECT dojo.run_file('schema.sql')$$
);

SELECT dojo.script_eq(
    'every played match has as many goal rows as goals',
    'load.sql',
    $$SELECT count(*) FROM matches m
      WHERE m.home_goals IS NOT NULL
        AND m.home_goals + m.away_goals <> (SELECT count(*) FROM goals g WHERE g.match_id = m.id)$$,
    $$VALUES (0)$$,
    setup => $$SELECT dojo.run_file('schema.sql')$$
);

SELECT dojo.script_eq(
    'own goals are marked',
    'load.sql',
    $$SELECT count(*) FILTER (WHERE own_goal), count(*) FILTER (WHERE NOT own_goal) FROM goals$$,
    $$VALUES (5, 63)$$,
    setup => $$SELECT dojo.run_file('schema.sql')$$
);

SELECT dojo.script_eq(
    'squad changes add newcomers and move a shirt',
    'squad_changes.sql',
    $$SELECT t.name, p.name, p.shirt_number
      FROM players p JOIN teams t ON t.id = p.team_id
      WHERE t.name IN ('Coldwater City', 'Fairhaven Town')$$,
    $$VALUES ('Coldwater City', 'Ivan Petrov', 1), ('Coldwater City', 'Jack Silva', 4),
             ('Coldwater City', 'Leo Walsh', 7), ('Coldwater City', 'Zico Barros', 8),
             ('Coldwater City', 'Kai Tanaka', 9),
             ('Fairhaven Town', 'Uli Holm', 1), ('Fairhaven Town', 'Vic Ito', 4),
             ('Fairhaven Town', 'Will Jansen', 9), ('Fairhaven Town', 'Yuri Kerr', 10),
             ('Fairhaven Town', 'Ola Strand', 11)$$,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('load.sql')$$
);

SELECT dojo.script_ok(
    'a player who changes shirt keeps their goals',
    'squad_changes.sql',
    $$SELECT (SELECT count(*) FROM players) = 26
         AND (SELECT count(*) FROM goals g JOIN players p ON p.id = g.player_id
              WHERE p.name = 'Leo Walsh') = (SELECT n FROM before)$$,
    'Leo Walsh must stay the same row (UPDATE on conflict, not DELETE and INSERT), and the league must have 26 players',
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('load.sql');
               CREATE TEMP TABLE before AS
                   SELECT count(*) AS n FROM goals g JOIN players p ON p.id = g.player_id
                   WHERE p.name = 'Leo Walsh'$$
);

SELECT dojo.script_eq(
    'running the squad changes twice changes nothing more',
    'squad_changes.sql',
    $$SELECT count(*), sum(shirt_number) FROM players$$,
    $$SELECT count(*), sum(shirt_number) FROM players_after_once$$,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('load.sql');
               SELECT dojo.run_file('squad_changes.sql');
               CREATE TEMP TABLE players_after_once AS SELECT * FROM players$$
);

SELECT dojo.script_eq(
    'round 9 gets its scores',
    'record_results.sql',
    $$SELECT h.name, a.name, m.home_goals, m.away_goals
      FROM matches m
      JOIN teams h ON h.id = m.home_team_id
      JOIN teams a ON a.id = m.away_team_id
      WHERE m.round = 9$$,
    $$VALUES ('Ashford Athletic', 'Coldwater City', 2, 2), ('Bramble United', 'Dunmore Rangers', 1, 0),
             ('Eastgate Wanderers', 'Fairhaven Town', 0, 3)$$,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('load.sql')$$
);

SELECT dojo.script_eq(
    'recording a round touches no other match',
    'record_results.sql',
    $$SELECT round, count(home_goals) FROM matches GROUP BY round$$,
    $$VALUES (1, 3), (2, 3), (3, 3), (4, 3), (5, 3), (6, 3), (7, 3), (8, 3), (9, 3), (10, 0)$$,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('load.sql')$$
);

SELECT dojo.script_eq(
    'results find their match by round and both names',
    'record_results.sql',
    $$SELECT m.round, h.name, a.name, m.home_goals, m.away_goals
      FROM matches m
      JOIN teams h ON h.id = m.home_team_id
      JOIN teams a ON a.id = m.away_team_id
      WHERE h.name = 'Coldwater City' AND a.name IN ('Dunmore Rangers', 'Fairhaven Town')$$,
    $$VALUES (1, 'Coldwater City', 'Dunmore Rangers', 2, 1), (10, 'Coldwater City', 'Fairhaven Town', 0, 1)$$,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('load.sql');
               INSERT INTO import_results VALUES ('10', ' Coldwater City', 'Dunmore Rangers', '4-4'),
                                                 ('10', 'Coldwater City ', 'Fairhaven Town', '0-1')$$
);
