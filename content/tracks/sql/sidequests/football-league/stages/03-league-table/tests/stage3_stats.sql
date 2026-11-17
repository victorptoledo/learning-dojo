-- Stage 3 contract (second part): top_scorers.sql, never_scored.sql and rounds.sql, after schema and load.

SELECT dojo.query_eq(
    'the top scorers, with everyone level with the fifth',
    'top_scorers.sql',
    $$VALUES ('Otto Abbot', 'Dunmore Rangers', 7), ('Kai Tanaka', 'Coldwater City', 6),
             ('Sam Ferro', 'Eastgate Wanderers', 6), ('Hugo Novak', 'Bramble United', 5),
             ('Leo Walsh', 'Coldwater City', 5), ('Will Jansen', 'Fairhaven Town', 5)$$,
    ordered => true,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('load.sql')$$
);

SELECT dojo.query_eq(
    'exactly five when nobody ties the fifth',
    'top_scorers.sql',
    $$VALUES ('Otto Abbot', 'Dunmore Rangers', 7), ('Hugo Novak', 'Bramble United', 6),
             ('Kai Tanaka', 'Coldwater City', 6), ('Sam Ferro', 'Eastgate Wanderers', 6),
             ('Leo Walsh', 'Coldwater City', 5)$$,
    ordered => true,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('load.sql');
               DELETE FROM goals WHERE id = (SELECT min(g.id) FROM goals g JOIN players p ON p.id = g.player_id
                                             WHERE p.name = 'Will Jansen');
               INSERT INTO goals (match_id, player_id, minute)
               SELECT m.id, p.id, 90 FROM players p, matches m
               WHERE p.name = 'Hugo Novak' AND m.round = 9
                 AND m.home_team_id = p.team_id$$
);

SELECT dojo.query_eq(
    'players with only own goals never scored',
    'never_scored.sql',
    $$VALUES ('Ashford Athletic', 'Ben Hale', 4), ('Bramble United', 'Eli Lindqvist', 1),
             ('Coldwater City', 'Ivan Petrov', 1), ('Coldwater City', 'Jack Silva', 4),
             ('Eastgate Wanderers', 'Quinn Duarte', 1), ('Fairhaven Town', 'Yuri Kerr', 10)$$,
    ordered => true,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('load.sql')$$
);

SELECT dojo.query_eq(
    'a newcomer has not scored yet',
    'never_scored.sql',
    $$VALUES ('Ashford Athletic', 'Ben Hale', 4), ('Bramble United', 'Eli Lindqvist', 1),
             ('Coldwater City', 'Ivan Petrov', 1), ('Coldwater City', 'Jack Silva', 4),
             ('Coldwater City', 'Zico Barros', 8),
             ('Eastgate Wanderers', 'Quinn Duarte', 1), ('Fairhaven Town', 'Yuri Kerr', 10)$$,
    ordered => true,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('load.sql');
               INSERT INTO players (team_id, name, shirt_number)
               SELECT id, 'Zico Barros', 8 FROM teams WHERE name = 'Coldwater City'$$
);

SELECT dojo.query_eq(
    'every round, played or not',
    'rounds.sql',
    $$VALUES (1, date '2026-08-01', 3, 11, 3.67), (2, date '2026-08-08', 3, 7, 2.33),
             (3, date '2026-08-15', 3, 8, 2.67), (4, date '2026-08-22', 3, 11, 3.67),
             (5, date '2026-09-01', 3, 9, 3.00), (6, date '2026-09-08', 3, 6, 2.00),
             (7, date '2026-09-15', 3, 7, 2.33), (8, date '2026-09-22', 3, 9, 3.00),
             (9, date '2026-10-01', 0, 0, NULL), (10, date '2026-10-08', 0, 0, NULL)$$,
    ordered => true,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('load.sql')$$
);

SELECT dojo.query_eq(
    'a round half played averages its played matches',
    'rounds.sql',
    $$VALUES (8, date '2026-09-22', 3, 9, 3.00), (9, date '2026-09-30', 1, 5, 5.00),
             (10, date '2026-10-08', 0, 0, NULL)$$,
    ordered => true,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('load.sql');
               UPDATE matches SET home_goals = 4, away_goals = 1, played_on = '2026-09-30'
               WHERE id = (SELECT min(id) FROM matches WHERE round = 9);
               DELETE FROM matches WHERE round < 8$$
);
