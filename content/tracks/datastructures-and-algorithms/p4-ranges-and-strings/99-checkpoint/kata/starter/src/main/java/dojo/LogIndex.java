package dojo;

/**
 * An editable log that counts watched patterns per line and answers range questions over the
 * line numbers.
 */
public final class LogIndex {

    /** Starts counting a pattern in every line, existing ones included; watching it again does nothing. */
    public void watch(String pattern) {
        throw new UnsupportedOperationException("todo watch " + pattern);
    }

    /** Adds a line at the end and returns its number, counting from 0. */
    public int append(String line) {
        throw new UnsupportedOperationException("todo append");
    }

    /** Replaces the text of an existing line. */
    public void replace(int number, String line) {
        throw new UnsupportedOperationException("todo replace " + number);
    }

    /** The number of lines. */
    public int lines() {
        throw new UnsupportedOperationException("todo lines");
    }

    /** The text of a line. */
    public String line(int number) {
        throw new UnsupportedOperationException("todo line " + number);
    }

    /** Occurrences of a watched pattern, overlapping ones included, in the lines [from, to). */
    public long count(String pattern, int from, int to) {
        throw new UnsupportedOperationException("todo count");
    }

    /** The first line at or after {@code from} that contains the pattern, or -1. */
    public int firstLineWith(String pattern, int from) {
        throw new UnsupportedOperationException("todo firstLineWith");
    }

    /** The line of [from, to) with the most occurrences, the first one on ties. */
    public int busiestLine(String pattern, int from, int to) {
        throw new UnsupportedOperationException("todo busiestLine");
    }
}
