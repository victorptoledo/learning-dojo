-- The library of this checkpoint. The tests load this file into a fresh PostgreSQL 17.
CREATE TABLE authors (
    id   integer PRIMARY KEY,
    name text NOT NULL
);

CREATE TABLE books (
    id        integer PRIMARY KEY,
    title     text NOT NULL,
    author_id integer NOT NULL REFERENCES authors (id),
    year      integer NOT NULL
);

CREATE TABLE members (
    id     integer PRIMARY KEY,
    name   text NOT NULL,
    joined date NOT NULL
);

-- A loan is open while returned_on is NULL. due_on is the last day to return it.
CREATE TABLE loans (
    id          integer PRIMARY KEY,
    book_id     integer NOT NULL REFERENCES books (id),
    member_id   integer NOT NULL REFERENCES members (id),
    borrowed_on date NOT NULL,
    due_on      date NOT NULL,
    returned_on date
);

CREATE TABLE fees (
    loan_id   integer PRIMARY KEY REFERENCES loans (id),
    member_id integer NOT NULL REFERENCES members (id),
    amount    numeric(8, 2) NOT NULL CHECK (amount > 0)
);

INSERT INTO authors (id, name) VALUES
    (1, 'Ursula K. Le Guin'),
    (2, 'Isaac Asimov'),
    (3, 'Octavia E. Butler'),
    (4, 'Ted Chiang');

INSERT INTO books (id, title, author_id, year) VALUES
    (1, 'A Wizard of Earthsea', 1, 1968),
    (2, 'The Left Hand of Darkness', 1, 1969),
    (3, 'The Dispossessed', 1, 1974),
    (4, 'Foundation', 2, 1951),
    (5, 'I, Robot', 2, 1950),
    (6, 'Kindred', 3, 1979),
    (7, 'Parable of the Sower', 3, 1993),
    (8, 'Stories of Your Life and Others', 4, 2002);

INSERT INTO members (id, name, joined) VALUES
    (1, 'Mia Chen', '2024-03-01'),
    (2, 'Noah Silva', '2025-01-15'),
    (3, 'Olivia Costa', '2025-06-30'),
    (4, 'Pedro Alves', '2026-10-01');

INSERT INTO loans (id, book_id, member_id, borrowed_on, due_on, returned_on) VALUES
    (1, 1, 1, '2026-09-01', '2026-09-15', '2026-09-14'),
    (2, 4, 1, '2026-10-20', '2026-11-03', NULL),
    (3, 6, 2, '2026-10-01', '2026-10-15', '2026-10-20'),
    (4, 2, 2, '2026-11-01', '2026-11-15', NULL),
    (5, 7, 3, '2026-10-10', '2026-10-24', NULL),
    (6, 1, 3, '2026-10-12', '2026-10-26', NULL),
    (7, 5, 2, '2026-08-01', '2026-08-15', '2026-08-15'),
    (8, 6, 1, '2026-10-25', '2026-11-08', '2026-11-10'),
    (9, 4, 3, '2026-07-01', '2026-07-15', '2026-07-30'),
    (10, 3, 4, '2026-10-05', '2026-10-19', '2026-10-18');
