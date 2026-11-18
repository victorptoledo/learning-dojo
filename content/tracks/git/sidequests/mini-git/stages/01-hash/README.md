Every Git object is named by the SHA-1 of a header and its content. This stage computes those
names **without Git**: in this stage the tests' `git` allows no subcommand at all.

**`minigit.sh hash blob FILE`** prints the id Git gives the file's content
(`git hash-object FILE`): the SHA-1 of `blob`, a space, the size in bytes in decimal, a NUL
byte, then the bytes of the file.

```text
$ printf 'hello\n' > hello.txt
$ bash minigit.sh hash blob hello.txt
ce013625030ba8dba906f756967f9e9ca394464a
```

**`minigit.sh hash tree DIR`** prints the id of the tree Git would make from the folder (what
`git add -A` and `git write-tree` give in a repository with exactly those files). A tree
object is `tree SIZE\0` followed by one entry per file or subfolder:

```text
MODE NAME\0ID
```

where `MODE` is ASCII (`100644` for a file, `100755` when the owner may execute it, `120000`
for a symbolic link, whose blob is the link's target, and `40000` for a subfolder), `NAME` is
the entry's name and `ID` is the **20 raw bytes** of the object's id, not 40 hex digits.

- Entries are sorted by name in byte order, where a **subfolder sorts as if its name ended
  in `/`**: `a.txt` comes before the folder `a`, because `.` is before `/`.
- Git does not track folders, only files: a folder with no files in it (at any depth) is not
  an entry. An empty `DIR` is the empty tree, `4b825dc642cb6eb9a060e54bf8d69288fbee4904`.
- A file or folder named `.git` is never an entry.
- Names may contain spaces or start with a dash; they never contain a newline.

Bash variables cannot hold NUL bytes, so build objects in files: `printf` writes `\0` and,
from two hex digits, `\xNN`. `sha1sum` reads standard input.

Errors: no command, an unknown one or a wrong number of arguments prints
`usage: minigit.sh COMMAND ARGS` on stderr and exits `2`; `hash` with a type other than `blob`
or `tree`, a `FILE` that is not a regular file or a `DIR` that is not a folder is the same
usage error.
