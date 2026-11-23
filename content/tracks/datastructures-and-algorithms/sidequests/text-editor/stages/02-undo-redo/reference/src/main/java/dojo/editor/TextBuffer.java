package dojo.editor;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A gap buffer: the text before the cursor at the start of {@code chars}, the text after it at
 * the end, and the free gap {@code [gapStart, gapEnd)} between them. The cursor is
 * {@code gapStart}. Undo and redo are two stacks of steps; each step stores only the text it
 * inserted or removed.
 */
public final class TextBuffer {

    private static final int MIN_CAPACITY = 16;

    private char[] chars;
    private int gapStart;
    private int gapEnd;

    private final Deque<Step> undo = new ArrayDeque<>();
    private final Deque<Step> redo = new ArrayDeque<>();
    /** The step that the next edit of the same kind may join, or null. */
    private Step open;

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
        if (position != gapStart) {
            open = null;
        }
        moveGap(position);
    }

    public void insert(String text) {
        if (text == null) {
            throw new IllegalArgumentException("the text is null");
        }
        if (text.isEmpty()) {
            return;
        }
        int before = gapStart;
        rawInsert(text);
        if (open != null && open.kind == Kind.INSERT && !startsWord(open, text)) {
            open.text.append(text);
        } else {
            push(new Step(Kind.INSERT, before, text, before));
        }
        open.after = gapStart;
    }

    public int deleteBackward(int count) {
        requireCount(count);
        int removed = Math.min(count, gapStart);
        if (removed == 0) {
            return 0;
        }
        String text = new String(chars, gapStart - removed, removed);
        int before = gapStart;
        gapStart -= removed;
        if (open != null && open.kind == Kind.DELETE_BACKWARD) {
            open.text.insert(0, text);
            open.position = gapStart;
        } else {
            push(new Step(Kind.DELETE_BACKWARD, gapStart, text, before));
        }
        open.after = gapStart;
        return removed;
    }

    public int deleteForward(int count) {
        requireCount(count);
        int removed = Math.min(count, chars.length - gapEnd);
        if (removed == 0) {
            return 0;
        }
        String text = new String(chars, gapEnd, removed);
        gapEnd += removed;
        if (open != null && open.kind == Kind.DELETE_FORWARD) {
            open.text.append(text);
        } else {
            push(new Step(Kind.DELETE_FORWARD, gapStart, text, gapStart));
        }
        open.after = gapStart;
        return removed;
    }

    public boolean undo() {
        open = null;
        Step step = undo.pollLast();
        if (step == null) {
            return false;
        }
        if (step.kind == Kind.INSERT) {
            rawDelete(step.position, step.text.length());
        } else {
            moveGap(step.position);
            rawInsert(step.text);
        }
        moveGap(step.before);
        redo.addLast(step);
        return true;
    }

    public boolean redo() {
        open = null;
        Step step = redo.pollLast();
        if (step == null) {
            return false;
        }
        if (step.kind == Kind.INSERT) {
            moveGap(step.position);
            rawInsert(step.text);
        } else {
            rawDelete(step.position, step.text.length());
        }
        moveGap(step.after);
        undo.addLast(step);
        return true;
    }

    public int undoSize() {
        return undo.size();
    }

    public int redoSize() {
        return redo.size();
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

    private enum Kind { INSERT, DELETE_BACKWARD, DELETE_FORWARD }

    /** One undo step: {@code text} was inserted at, or removed from, {@code position}. */
    private static final class Step {
        final Kind kind;
        int position;
        final StringBuilder text;
        final int before;
        int after;

        Step(Kind kind, int position, String text, int before) {
            this.kind = kind;
            this.position = position;
            this.text = new StringBuilder(text);
            this.before = before;
        }
    }

    private void push(Step step) {
        undo.addLast(step);
        redo.clear();
        open = step;
    }

    private static boolean startsWord(Step step, String text) {
        char last = step.text.charAt(step.text.length() - 1);
        return isBreak(text.charAt(0)) && !isBreak(last);
    }

    private static boolean isBreak(char c) {
        return c == ' ' || c == '\n';
    }

    private int gapSize() {
        return gapEnd - gapStart;
    }

    private static void requireCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("negative count " + count);
        }
    }

    private void rawInsert(CharSequence text) {
        ensureGap(text.length());
        for (int i = 0; i < text.length(); i++) {
            chars[gapStart + i] = text.charAt(i);
        }
        gapStart += text.length();
    }

    private void rawDelete(int position, int length) {
        moveGap(position);
        gapEnd += length;
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
