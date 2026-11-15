# Hello, Shell

Write `greet.sh`. It prints a welcome message for the name given as arguments.

```text
bash greet.sh Ada              ->  Welcome to the dojo, Ada!
bash greet.sh Ada Lovelace     ->  Welcome to the dojo, Ada Lovelace!   (every argument is part of the name)
bash greet.sh                  ->  Welcome to the dojo, stranger!       (no name: greet a stranger)
bash greet.sh ""               ->  Welcome to the dojo, stranger!       (an empty name counts as no name)
bash greet.sh --shout Ada      ->  WELCOME TO THE DOJO, ADA!            (--shout first: upper case)
```

The script always exits with status `0`.

What you start with:

```bash
#!/usr/bin/env bash
echo "todo: write greet.sh" >&2
exit 1
```

Replace those two lines. Tools you will need:

- `$1`, `$2`, ... are the arguments; `$#` is how many; `"$*"` is all of them joined by spaces.
- `shift` drops the first argument, so `$2` becomes `$1`.
- `[ -z "$name" ]` is true when the text is empty; `[ "$a" = "$b" ]` compares texts.
- `if ...; then ...; fi` runs commands only when the test succeeds.
- `${text^^}` is the text in upper case (or `echo "$text" | tr '[:lower:]' '[:upper:]'`).
- Quote every variable: `"$name"`. ShellCheck reports unquoted ones.

Run it yourself in a Linux shell: `bash greet.sh Ada`. There are 6 tests: 1 in
`tests/visible.bats` (open it to see how bats works) and 5 hidden ones, plus ShellCheck.
