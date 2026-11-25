# Files, Paths and Text I/O

Almost every service reads or writes files: configuration, imports, exports, reports. Since
Java 7 the `java.nio.file` package (NIO.2) does this with two types, `Path` and `Files`, and
since Java 11 most everyday jobs are one line. The old `java.io.File` still works, but new
code should not start there.

## Paths: where a file is

A `Path` is a location, not a file: it may not exist yet.

```java
Path dir = Path.of("imports");                  // relative to the working directory
Path file = dir.resolve("2026-11.csv");          // imports/2026-11.csv
Path absolute = file.toAbsolutePath().normalize();
String name = file.getFileName().toString();     // "2026-11.csv"
Path parent = file.getParent();                  // imports
```

`resolve` joins paths with the right separator on every system, so never build paths by
concatenating strings with `"/"`. `normalize()` removes `.` and `..`; a path that should stay
inside a folder must be checked **after** normalising:

```java
Path target = root.resolve(userInput).normalize();
if (!target.startsWith(root)) {
    throw new IllegalArgumentException("path escapes the upload folder");
}
```

## Reading and writing text

```java
String all = Files.readString(file);                          // UTF-8 by default
List<String> lines = Files.readAllLines(file);                // small files
Files.writeString(out, report);                               // create or replace
Files.writeString(log, line + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
```

Since Java 18 the default charset is **UTF-8** everywhere (JEP 400), and the `Files` methods
always used UTF-8. Still, name the charset when a file comes from outside:
`Files.readString(file, StandardCharsets.ISO_8859_1)`. A wrong charset does not fail loudly;
it turns `ã` into `Ã£`.

## Large files: stream, do not load

`readAllLines` keeps the whole file in memory. For a file of any size, read it as a lazy
stream of lines, and close it:

```java
try (Stream<String> lines = Files.lines(file)) {
    long errors = lines.filter(line -> line.contains("ERROR")).count();
}
```

`Files.lines`, `Files.list` and `Files.walk` hold an open file handle until the stream is
closed, so **always** use them in try-with-resources. For line-by-line work with state,
`Files.newBufferedReader(file)` and a `while ((line = reader.readLine()) != null)` loop is
just as good.

## Listing a folder

```java
try (Stream<Path> entries = Files.list(dir)) {
    List<Path> csvs = entries
            .filter(Files::isRegularFile)
            .filter(p -> p.getFileName().toString().endsWith(".csv"))
            .sorted()
            .toList();
}
```

`Files.list` returns entries **in no particular order**. Sort them whenever the result must be
repeatable (reports, tests). `Files.walk(dir)` goes into subfolders; `Files.find` adds a
filter on attributes.

## Writing safely: temp file and atomic move

A reader that opens a report while you are still writing it sees half a file, and a crash in
the middle leaves a broken one. Write to a temporary file **in the same folder**, then move it
over the target in one step:

```java
Path temp = Files.createTempFile(target.getParent(), ".report", ".tmp");
try {
    Files.writeString(temp, content);
    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
} finally {
    Files.deleteIfExists(temp);   // only left behind if the move failed
}
```

The move is atomic only inside one file system, which is why the temp file lives next to the
target and not in `/tmp`.

## Errors

`Files` methods throw `IOException` (checked) with a precise subtype: `NoSuchFileException`,
`FileAlreadyExistsException`, `AccessDeniedException`, `DirectoryNotEmptyException`. Catch
the subtype you can handle and let the rest travel up, wrapped in `UncheckedIOException` if a
lambda or a stream is in the way:

```java
try {
    return Files.readString(file);
} catch (NoSuchFileException e) {
    return "";                                  // a missing file is normal here
} catch (IOException e) {
    throw new UncheckedIOException("cannot read " + file, e);
}
```

## Testing with temporary folders

JUnit creates and deletes a folder for each test:

```java
@Test
void writes_the_report(@TempDir Path dir) throws IOException {
    Files.writeString(dir.resolve("a.csv"), "id,hours\n1,8\n");
    new Importer().run(dir);
    assertEquals("total,8\n", Files.readString(dir.resolve("report.csv")));
}
```

No test should write into the project folder or depend on files left by another test.

## Summary

- `Path` is a location; join with `resolve`, check escapes after `normalize`.
- `Files.readString`/`writeString` for small files; `Files.lines` in try-with-resources for
  large ones.
- UTF-8 is the default, but name the charset for outside files.
- Sort what `Files.list` returns.
- Write to a temp file in the same folder, then `ATOMIC_MOVE`.
- Catch the precise `IOException` subtype; use `@TempDir` in tests.
