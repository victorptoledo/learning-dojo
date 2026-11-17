With no argument, `calc` is a REPL: it reads expressions from stdin, one per line, until the
input ends.

- Each line prints its result on stdout, one line per result. Blank lines are skipped.
- A bad line prints `error: <message>` on **stderr** and the REPL goes on with the next line.
- `let <name> = <expression>` stores a variable and prints `<name> = <value>`. Names are
  letters, digits and `_`, starting with a letter. Later lines can use the name:
  `let rate = 2` then `rate * 21` prints `42`. `let` again replaces the value.
- `ans` is the result of the last line that succeeded (`let` lines too). Before any result it
  is an unknown variable.
- An unknown name is an error whose message contains `unknown variable 'name'`.
- At the end of the input the exit code is 0 when every line succeeded, 1 otherwise.

```text
$ printf '1 + 2\nlet x = ans * 10\nx / 4\ny\n' | calc
3
x = 30
7.5
error: unknown variable 'y'        (on stderr)
```

`calc "<expression>"` with an argument keeps working as before (it has no variables).
