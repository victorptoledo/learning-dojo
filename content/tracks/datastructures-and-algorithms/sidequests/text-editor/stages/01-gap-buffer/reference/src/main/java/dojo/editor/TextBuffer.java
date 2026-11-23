package dojo.editor;

/**
 * A gap buffer: the text before the cursor at the start of {@code chars}, the text after it at
 * the end, and the free gap {@code [gapStart, gapEnd)} between them. The cursor is
 * {@code gapStart}.
 */
public final class TextBuffer {

    private static final int MIN_CAPACITY = 16;

    private char[] chars;
    private int gapStart;
    private int gapEnd;

    public TextBuffer() {
        this("");
    }

    public TextBuffer(String initial) {
        if (initial == null) {
            throw new IllegalArgumentException("the initial text is null");
        }
        chars = new char[Math.max(MIN_CAPACITY, initial.length() * 2)];
        initial.getChars(0, initial.length(), chars, 0);
        gapStart = initial.length();
        gapEnd = chars.length;
    }

    public int length() {
        return chars.length - gapSize();
    }

    public int cursor() {
        return gapStart;
    }

    public void moveTo(int position) {
        if (position < 0 || position > length()) {
            throw new IllegalArgumentException("position " + position + " outside 0.." + length());
        }
        moveGap(position);
    }

    public void insert(String text) {
        if (text == null) {
            throw new IllegalArgumentException("the text is null");
        }
        ensureGap(text.length());
        text.getChars(0, text.length(), chars, gapStart);
        gapStart += text.length();
    }

    public int deleteBackward(int count) {
        requireCount(count);
        int removed = Math.min(count, gapStart);
        gapStart -= removed;
        return removed;
    }

    public int deleteForward(int count) {
        requireCount(count);
        int removed = Math.min(count, chars.length - gapEnd);
        gapEnd += removed;
        return removed;
    }

    public char charAt(int index) {
        if (index < 0 || index >= length()) {
            throw new IndexOutOfBoundsException("index " + index + " outside 0.." + (length() - 1));
        }
        return index < gapStart ? chars[index] : chars[index + gapSize()];
    }

    public String substring(int from, int to) {
        if (from < 0 || to > length() || from > to) {
            throw new IndexOutOfBoundsException("range [" + from + ", " + to + ") outside 0.." + length());
        }
        var out = new StringBuilder(to - from);
        if (from < gapStart) {
            out.append(chars, from, Math.min(to, gapStart) - from);
        }
        if (to > gapStart) {
            int start = Math.max(from, gapStart) + gapSize();
            out.append(chars, start, to + gapSize() - start);
        }
        return out.toString();
    }

    public String text() {
        return substring(0, length());
    }

    private int gapSize() {
        return gapEnd - gapStart;
    }

    private static void requireCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("negative count " + count);
        }
    }

    /** Copies the characters between the cursor and {@code position} across the gap. */
    private void moveGap(int position) {
        if (position < gapStart) {
            int n = gapStart - position;
            System.arraycopy(chars, position, chars, gapEnd - n, n);
            gapStart -= n;
            gapEnd -= n;
        } else if (position > gapStart) {
            int n = position - gapStart;
            System.arraycopy(chars, gapEnd, chars, gapStart, n);
            gapStart += n;
            gapEnd += n;
        }
    }

    /** Doubles the array when the gap cannot take {@code needed} more characters. */
    private void ensureGap(int needed) {
        if (gapSize() >= needed) {
            return;
        }
        int capacity = Math.max(chars.length * 2, length() + needed + MIN_CAPACITY);
        char[] grown = new char[capacity];
        System.arraycopy(chars, 0, grown, 0, gapStart);
        int tail = chars.length - gapEnd;
        System.arraycopy(chars, gapEnd, grown, capacity - tail, tail);
        gapEnd = capacity - tail;
        chars = grown;
    }
}
