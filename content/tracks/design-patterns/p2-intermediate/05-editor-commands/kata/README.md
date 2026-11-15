# Editor Commands

`TextEditor` supports undo by copying the whole text before every edit and keeping a list of
history strings in step by hand. There is no redo, every new kind of edit has to remember all
three chores, and nothing can be grouped into one undoable step.

Refactor it to the **Command** pattern: each edit is an object that knows how to run and how
to undo itself, an `Editor` (the invoker) keeps the undo and redo stacks, and a `Macro`
(a **composite** command) groups commands into one step. `TextEditor` keeps its methods for
its callers, gains `redo()`, and becomes a thin layer over `Editor`.

The visible tests pin today's behaviour of `TextEditor` and pass before you start.

## The target design

| Type | What it does |
| --- | --- |
| `final class Document` | the receiver: `insert(int at, String fragment)`, `String delete(int at, int length)` (returns what it removed), `String text()`, `replaceWith(String newText)`. Out-of-range positions throw `IndexOutOfBoundsException` and change nothing. |
| `interface Command` | `void execute(Document)`, `void undo(Document)`, `String describe()` |
| `Insert(int at, String fragment)` | describes itself as `"insert " + length` |
| `Delete(int at, int length)` | remembers what it removed so undo restores exactly that; `"delete " + length` |
| `ReplaceAll(String target, String replacement)` | `"replace " + target + "->" + replacement` |
| `Macro(String name, List<Command> steps)` | runs the steps in order and undoes them in reverse; if a step throws, undoes the steps already run and rethrows; describes itself by its name |
| `final class Editor` | `run(Command)`, `undo()`, `redo()`, `String text()`, `List<String> history()` (descriptions of the commands in effect, oldest first) |

Rules for `Editor`:

- `run` executes the command; only if it succeeds is it recorded, and the redo stack is
  cleared.
- `undo` with nothing to undo throws `IllegalStateException("nothing to undo")`; `redo`
  with nothing to redo throws `IllegalStateException("nothing to redo")`.
- `Editor` never copies the text: no `String` fields, no snapshots. Commands undo themselves.

## What the grading tests check

- The old `TextEditor` behaviour, plus `redo()`.
- Undo and redo in both directions, the redo stack cleared by a new command, and failed
  commands not recorded.
- A command the grader writes (it upper-cases the text) runs and undoes through your
  `Editor` without any change to it.
- Macros undo and redo as one step and roll back when a step fails.
- `Editor` holds no `String` state and has no `instanceof`, `switch` or `toString()`.
