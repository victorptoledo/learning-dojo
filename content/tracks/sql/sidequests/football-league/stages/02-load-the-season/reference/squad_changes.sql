-- New players join their team; a player already in the team keeps their row (and their goals)
-- and only changes shirt.
INSERT INTO players (team_id, name, shirt_number)
SELECT t.id, trim(c.player), c.shirt::integer
FROM import_squad_changes c
JOIN teams t ON t.name = trim(c.team)
ON CONFLICT (team_id, name) DO UPDATE
SET shirt_number = excluded.shirt_number;
