package dojo;

import java.util.List;

/** The old API, kept for its callers: each method builds a command and hands it to the editor. */
public class TextEditor {
    private final Editor editor = new Editor();

    public void insert(int at, String fragment) {
        editor.run(new Insert(at, fragment));
    }

    public void delete(int at, int length) {
        editor.run(new Delete(at, length));
    }

    public void replaceAll(String target, String replacement) {
        editor.run(new ReplaceAll(target, replacement));
    }

    public void undo() {
        editor.undo();
    }

    public void redo() {
        editor.redo();
    }

    public String text() {
        return editor.text();
    }

    public List<String> history() {
        return editor.history();
    }
}
