# Sidequest: Text editor buffer

Every text editor keeps the document in a data structure built for one thing: a person typing
at a cursor. A plain `String` copies the whole text on every key, and even a `StringBuilder`
shifts everything after the cursor each time you type in the middle of a file. Editors use a
**gap buffer** (Emacs), a piece table (VS Code) or a rope (Zed, Xi) instead.

Build `dojo.editor.TextBuffer`, the buffer of a small editor. The starter declares the class and
its first methods; how you store the text is yours, and the contract tests only call the public
methods. They also measure: a megabyte of text and hundreds of thousands of keystrokes must run
in a few seconds, inside a 256 MB heap, so the wrong structure fails on time or memory, not on a
code review.

- **Stage 1:** a gap buffer: insert and delete at the cursor in O(1) amortised, move the cursor
  by paying only for the distance.
- **Stage 2:** undo and redo with two stacks of edits (never copies of the text), grouping typing
  by words the way editors do.
- **Stage 3:** find and replace all in linear time on the buffer itself, and a line index.

Positions are `char` indexes from `0` to `length()`; the cursor sits *between* characters.
Use the JDK only, without `StringBuilder.insert`/`delete` in the middle of the text or any
other structure that shifts the whole tail on each edit. JUnit is there for your own tests.
