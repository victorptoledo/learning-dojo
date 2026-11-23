package dojo.editor;

/** The text of an editor and its cursor. See README.md. */
public final class TextBuffer {

    public TextBuffer() {
        this("");
    }

    public TextBuffer(String initial) {
        // todo: store the text, cursor at its end
    }

    public int length() {
        throw new UnsupportedOperationException("todo");
    }

    public int cursor() {
        throw new UnsupportedOperationException("todo");
    }

    public void moveTo(int position) {
        throw new UnsupportedOperationException("todo");
    }

    public void insert(String text) {
        throw new UnsupportedOperationException("todo");
    }

    public int deleteBackward(int count) {
        throw new UnsupportedOperationException("todo");
    }

    public int deleteForward(int count) {
        throw new UnsupportedOperationException("todo");
    }

    public char charAt(int index) {
        throw new UnsupportedOperationException("todo");
    }

    public String substring(int from, int to) {
        throw new UnsupportedOperationException("todo");
    }

    public String text() {
        throw new UnsupportedOperationException("todo");
    }
}
