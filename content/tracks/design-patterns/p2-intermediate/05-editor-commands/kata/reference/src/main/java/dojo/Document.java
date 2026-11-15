package dojo;

/** The receiver: the text the commands work on. It knows nothing about undo. */
public final class Document {
    private final StringBuilder text = new StringBuilder();

    public void insert(int at, String fragment) {
        if (at < 0 || at > text.length()) {
            throw new IndexOutOfBoundsException("position " + at);
        }
        text.insert(at, fragment);
    }

    /** Removes {@code length} characters at {@code at} and returns them. */
    public String delete(int at, int length) {
        if (at < 0 || length < 0 || at + length > text.length()) {
            throw new IndexOutOfBoundsException("range " + at + "+" + length);
        }
        String removed = text.substring(at, at + length);
        text.delete(at, at + length);
        return removed;
    }

    public String text() {
        return text.toString();
    }

    public void replaceWith(String newText) {
        text.setLength(0);
        text.append(newText);
    }
}
