-- Stage 3 contract: standings.sql is the league table, in the order of the standings.

SELECT dojo.query_eq(
    'the table after eight rounds',
    'standings.sql',
    $$VALUES ('Ashford Athletic', 8, 5, 0, 3, 12, 9, 3, 15),
             ('Bramble United', 8, 4, 1, 3, 10, 9, 1, 13),
             ('Coldwater City', 8, 4, 0, 4, 12, 11, 1, 12),
             ('Eastgate Wanderers', 8, 3, 1, 4, 10, 11, -1, 10),
             ('Dunmore Rangers', 8, 3, 1, 4, 14, 16, -2, 10),
             ('Fairhaven Town', 8, 3, 1, 4, 10, 12, -2, 10)$$,
    ordered => true,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('load.sql')$$
);

SELECT dojo.query_eq(
    'the table follows a new round',
    'standings.sql',
    $$VALUES ('Ashford Athletic', 9, 5, 1, 3, 14, 11, 3, 16),
             ('Bramble United', 9, 5, 1, 3, 11, 9, 2, 16),
             ('Coldwater City', 9, 4, 1, 4, 14, 13, 1, 13),
             ('Fairhaven Town', 9, 4, 1, 4, 13, 12, 1, 13),
             ('Dunmore Rangers', 9, 3, 1, 5, 14, 17, -3, 10),
             ('Eastgate Wanderers', 9, 3, 1, 5, 10, 14, -4, 10)$$,
    ordered => true,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('load.sql');
               UPDATE matches m SET home_goals = v.hg, away_goals = v.ag
               FROM (VALUES ('Ashford Athletic', 2, 2), ('Bramble United', 1, 0),
                            ('Eastgate Wanderers', 0, 3)) AS v(home, hg, ag)
               JOIN teams h ON h.name = v.home
               WHERE m.round = 9 AND m.home_team_id = h.id$$
);

SELECT dojo.query_eq(
    'a goalless draw is played and drawn',
    'standings.sql',
    $$VALUES ('Ashford Athletic', 9, 5, 1, 3, 12, 9, 3, 16),
             ('Bramble United', 9, 4, 2, 3, 10, 9, 1, 14),
             ('Coldwater City', 8, 4, 0, 4, 12, 11, 1, 12),
             ('Eastgate Wanderers', 8, 3, 1, 4, 10, 11, -1, 10),
             ('Dunmore Rangers', 8, 3, 1, 4, 14, 16, -2, 10),
             ('Fairhaven Town', 8, 3, 1, 4, 10, 12, -2, 10)$$,
    ordered => true,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('load.sql');
               UPDATE matches SET home_goals = 0, away_goals = 0
               WHERE round = 10 AND home_team_id = (SELECT id FROM teams WHERE name = 'Bramble United')$$
);

SELECT dojo.query_eq(
    'clubs that have not played yet appear with zeros, by name',
    'standings.sql',
    $$VALUES ('Ashford Athletic', 8, 5, 0, 3, 12, 9, 3, 15),
             ('Bramble United', 8, 4, 1, 3, 10, 9, 1, 13),
             ('Coldwater City', 8, 4, 0, 4, 12, 11, 1, 12),
             ('Eastgate Wanderers', 8, 3, 1, 4, 10, 11, -1, 10),
             ('Dunmore Rangers', 8, 3, 1, 4, 14, 16, -2, 10),
             ('Fairhaven Town', 8, 3, 1, 4, 10, 12, -2, 10),
             ('Glenrock Albion', 0, 0, 0, 0, 0, 0, 0, 0),
             ('Harbour Hill', 0, 0, 0, 0, 0, 0, 0, 0)$$,
    ordered => true,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('load.sql');
               INSERT INTO teams (name, city) VALUES ('Harbour Hill', 'Harbour'), ('Glenrock Albion', 'Glenrock')$$
);

SELECT dojo.query_eq(
    'goals scored break a tie on points and difference',
    'standings.sql',
    $$VALUES ('Hilltop', 1, 0, 1, 0, 3, 3, 0, 1), ('Lowfield', 1, 0, 1, 0, 3, 3, 0, 1),
             ('Easton', 1, 0, 1, 0, 1, 1, 0, 1), ('Weston', 1, 0, 1, 0, 1, 1, 0, 1)$$,
    ordered => true,
    setup => $$SELECT dojo.run_file('schema.sql');
               INSERT INTO teams (name, city) VALUES ('Weston', 'W'), ('Easton', 'E'), ('Lowfield', 'L'), ('Hilltop', 'H');
               INSERT INTO matches (round, played_on, home_team_id, away_team_id, home_goals, away_goals)
               SELECT 1, '2026-08-01', h.id, a.id, v.g, v.g
               FROM (VALUES ('Weston', 'Easton', 1), ('Lowfield', 'Hilltop', 3)) AS v(home, away, g)
               JOIN teams h ON h.name = v.home JOIN teams a ON a.name = v.away$$
);
