-- The league's schema. Every rule lives in the database, so no buggy import can break it.

CREATE TABLE teams (
    id   integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name text NOT NULL UNIQUE,
    city text NOT NULL
);

CREATE TABLE players (
    id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    team_id      integer NOT NULL REFERENCES teams,
    name         text NOT NULL,
    shirt_number integer NOT NULL CHECK (shirt_number BETWEEN 1 AND 99),
    UNIQUE (team_id, name),
    UNIQUE (team_id, shirt_number)
);

CREATE TABLE matches (
    id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    round        integer NOT NULL CHECK (round >= 1),
    played_on    date NOT NULL,
    home_team_id integer NOT NULL REFERENCES teams,
    away_team_id integer NOT NULL REFERENCES teams,
    home_goals   integer CHECK (home_goals >= 0),
    away_goals   integer CHECK (away_goals >= 0),
    CHECK (home_team_id <> away_team_id),
    CHECK ((home_goals IS NULL) = (away_goals IS NULL)),
    UNIQUE (home_team_id, away_team_id)
);

CREATE TABLE goals (
    id        integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    match_id  integer NOT NULL REFERENCES matches ON DELETE CASCADE,
    player_id integer NOT NULL REFERENCES players,
    minute    integer NOT NULL CHECK (minute BETWEEN 1 AND 120),
    own_goal  boolean NOT NULL DEFAULT false
);
