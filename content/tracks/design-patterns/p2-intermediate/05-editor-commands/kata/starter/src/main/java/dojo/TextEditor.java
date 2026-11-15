package dojo;

import java.util.ArrayList;
import java.util.List;

/**
 * A tiny text editor with undo. Every edit copies the whole text first so that undo can put it
 * back, the history is a list of strings kept in step by hand, and there is no redo. A new kind
 * of edit means another method that remembers to do all three.
 */
public class TextEditor {
    private StringBuilder text = new StringBuilder();
    private final List<String> snapshots = new ArrayList<>();
    private final List<String> history = new ArrayList<>();

    public void insert(int at, String fragment) {
        if (at < 0 || at > text.length()) {
            throw new IndexOutOfBoundsException("position " + at);
        }
        snapshots.add(text.toString());
        text.insert(at, fragment);
        history.add("insert " + fragment.length());
    }

    public void delete(int at, int length) {
        if (at < 0 || length < 0 || at + length > text.length()) {
            throw new IndexOutOfBoundsException("range " + at + "+" + length);
        }
        snapshots.add(text.toString());
        text.delete(at, at + length);
        history.add("delete " + length);
    }

    public void replaceAll(String target, String replacement) {
        snapshots.add(text.toString());
        text = new StringBuilder(text.toString().replace(target, replacement));
        history.add("replace " + target + "->" + replacement);
    }

    public void undo() {
        if (snapshots.isEmpty()) {
            throw new IllegalStateException("nothing to undo");
        }
        text = new StringBuilder(snapshots.removeLast());
        history.removeLast();
    }

    public String text() {
        return text.toString();
    }

    public List<String> history() {
        return List.copyOf(history);
    }
}
