Every editor can take back what you did. The classic design is two **stacks of edits**: an edit
pushed on the undo stack knows how to revert itself (an insertion is reverted by deleting the
same text, a deletion by inserting back what it removed), undo pops it and pushes it on the redo
stack, and any new edit clears the redo stack. A stack holds the *edits*, never copies of the
text: that would cost a megabyte per keystroke.

Add to `TextBuffer`:

```java
public boolean undo()        // reverts the last step; false when there is nothing to undo
public boolean redo()        // reapplies the last undone step; false when there is none
public int undoSize()        // steps that undo can revert
public int redoSize()        // steps that redo can reapply
```

**Steps.** Pressing undo after typing a sentence should not remove one letter at a time, so edits
are grouped into steps the way editors do it:

- Every `insert`, `deleteBackward` and `deleteForward` that changes the text is an edit. Calls
  that change nothing (`insert("")`, deleting at an edge) are not, and change no stack.
- An edit **joins the current step** when it is the same kind as the previous edit (insert,
  backward delete or forward delete) and nothing happened in between: no `moveTo` to another
  position, no `undo`, no `redo`.
- Typing is grouped **by words**: an insert that starts with a space or a line break opens a new
  step, unless the current step already ends with a space or a line break. Typing `hello world`
  one character at a time makes two steps, `hello` and ` world`.
- The text given to the constructor is not a step.

**Cursor.** `undo` puts the cursor where it was before the step; `redo` puts it where it was
after the step.

```java
var buffer = new TextBuffer();
for (char c : "hello world".toCharArray()) buffer.insert(String.valueOf(c));
buffer.undo();                 // "hello", cursor 5
buffer.deleteBackward(1);      // "hell": a new step, and redo has nothing left
buffer.undo();                 // "hello"
buffer.undo();                 // ""
buffer.redo();                 // "hello", cursor 5
```

The tests make 50,000 steps on a text of 500,000 characters, then undo and redo all of them,
inside a 256 MB heap: keep the steps small.
