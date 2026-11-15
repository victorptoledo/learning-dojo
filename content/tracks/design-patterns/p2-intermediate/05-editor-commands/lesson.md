# Editor Commands

A small editor implements undo by copying the whole text before every edit, and has no redo.
You will turn each edit into a **command** that knows how to undo itself, give the editor undo
and redo stacks, and add **macros**: composite commands that run, undo and roll back as one
step. The old `TextEditor` API stays for its callers. Press **Enter** to create the kata folder
and open it.
