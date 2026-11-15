package dojo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** The invoker: runs commands and keeps the undo and redo stacks. It never copies the text. */
public final class Editor {
    private final Document document = new Document();
    private final Deque<Command> done = new ArrayDeque<>();
    private final Deque<Command> undone = new ArrayDeque<>();

    public void run(Command command) {
        command.execute(document);
        done.push(command);
        undone.clear();
    }

    public void undo() {
        if (done.isEmpty()) {
            throw new IllegalStateException("nothing to undo");
        }
        Command command = done.pop();
        command.undo(document);
        undone.push(command);
    }

    public void redo() {
        if (undone.isEmpty()) {
            throw new IllegalStateException("nothing to redo");
        }
        Command command = undone.pop();
        command.execute(document);
        done.push(command);
    }

    public String text() {
        return document.text();
    }

    /** Descriptions of the commands in effect, oldest first. */
    public List<String> history() {
        return done.reversed().stream().map(Command::describe).toList();
    }
}
