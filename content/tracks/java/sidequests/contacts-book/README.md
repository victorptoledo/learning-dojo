# Sidequest: Contacts book

Build a contacts book you run from the terminal. Every run reads the data file, does one
command and saves the file again, like a tiny database.

The only fixed piece is the entry point in `src/main/java/dojo/contacts/ContactsApp.java`:

```java
public static int run(String[] args, PrintStream out, PrintStream err)
```

It returns the exit code, prints results to `out` and problems to `err`; `main` just calls it.
The contract tests call `run` with arguments and read what it printed, exactly like a user at
the terminal would. Everything behind it is yours: a `record Contact`, a class that loads and
saves the file, one method or class per command...

Every command starts with `--file <path>`, the data file. A missing file is an empty book.
The format of the file is yours.

```text
contacts --file book.txt add "Ana Lima" ana@example.com "+55 11 91234-5678"
contacts --file book.txt list
```

Use the JDK only (no libraries besides JUnit for your own tests).
