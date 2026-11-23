package dojo.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.Random;
import org.junit.jupiter.api.Test;

class Stage1GapBufferTest {

    @Test
    void startsEmptyOrWithTheInitialTextAndTheCursorAtTheEnd() {
        var empty = new TextBuffer();
        assertEquals(0, empty.length());
        assertEquals(0, empty.cursor());
        assertEquals("", empty.text());

        var buffer = new TextBuffer("hello");
        assertEquals(5, buffer.length());
        assertEquals(5, buffer.cursor());
        assertEquals("hello", buffer.text());
    }

    @Test
    void insertsAtTheCursorAndMovesIt() {
        var buffer = new TextBuffer("held");
        buffer.moveTo(3);
        buffer.insert("lo worl");
        assertEquals("hello world", buffer.text());
        assertEquals(10, buffer.cursor());
        buffer.insert("");
        assertEquals("hello world", buffer.text());
        buffer.moveTo(0);
        buffer.insert(">> ");
        assertEquals(">> hello world", buffer.text());
        assertEquals(3, buffer.cursor());
        buffer.moveTo(buffer.length());
        buffer.insert("!");
        assertEquals(">> hello world!", buffer.text());
    }

    @Test
    void theReadmeExample() {
        var buffer = new TextBuffer("hello world");
        buffer.moveTo(5);
        buffer.insert(",");
        assertEquals("hello, world", buffer.text());
        assertEquals(6, buffer.cursor());
        assertEquals(1, buffer.deleteForward(1));
        assertEquals("hello,world", buffer.text());
        assertEquals(6, buffer.cursor());
        assertEquals(6, buffer.deleteBackward(99));
        assertEquals("world", buffer.text());
        assertEquals(0, buffer.cursor());
    }

    @Test
    void deletesStopAtTheEdges() {
        var buffer = new TextBuffer("abc");
        assertEquals(0, buffer.deleteForward(5), "nothing after the cursor at the end");
        assertEquals(2, buffer.deleteBackward(2));
        assertEquals("a", buffer.text());
        buffer.moveTo(0);
        assertEquals(0, buffer.deleteBackward(1), "nothing before the cursor at the start");
        assertEquals(0, buffer.deleteForward(0));
        assertEquals(1, buffer.deleteForward(10));
        assertEquals("", buffer.text());
        assertEquals(0, buffer.length());
    }

    @Test
    void charAtAndSubstringSeeAcrossTheGap() {
        var buffer = new TextBuffer("abcdefgh");
        buffer.moveTo(3);
        buffer.insert("XY");
        buffer.moveTo(4);
        assertEquals("abcXYdefgh", buffer.text());
        assertEquals('X', buffer.charAt(3));
        assertEquals('Y', buffer.charAt(4));
        assertEquals('d', buffer.charAt(5));
        assertEquals('h', buffer.charAt(9));
        assertEquals("cXYd", buffer.substring(2, 6));
        assertEquals("", buffer.substring(4, 4));
        assertEquals("abcX", buffer.substring(0, 4));
        assertEquals("Ydefgh", buffer.substring(4, 10));
    }

    @Test
    void badArgumentsAreRefused() {
        var buffer = new TextBuffer("abc");
        assertThrows(IllegalArgumentException.class, () -> new TextBuffer(null));
        assertThrows(IllegalArgumentException.class, () -> buffer.moveTo(-1));
        assertThrows(IllegalArgumentException.class, () -> buffer.moveTo(4));
        assertThrows(IllegalArgumentException.class, () -> buffer.insert(null));
        assertThrows(IllegalArgumentException.class, () -> buffer.deleteBackward(-1));
        assertThrows(IllegalArgumentException.class, () -> buffer.deleteForward(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.charAt(3));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.charAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.substring(2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.substring(0, 4));
        assertEquals("abc", buffer.text());
        assertEquals(3, buffer.cursor());
    }

    @Test
    void growsWellPastItsFirstCapacity() {
        var buffer = new TextBuffer();
        var expected = new StringBuilder();
        for (int i = 0; i < 20_000; i++) {
            char c = (char) ('a' + i % 26);
            buffer.insert(String.valueOf(c));
            expected.append(c);
        }
        assertEquals(expected.toString(), buffer.text());
        assertEquals(20_000, buffer.cursor());
    }

    @Test
    void randomEditsMatchAStringBuilder() {
        var random = new Random(7);
        var expected = new StringBuilder("the quick brown fox");
        int cursor = expected.length();
        var buffer = new TextBuffer(expected.toString());
        for (int step = 0; step < 20_000; step++) {
            switch (random.nextInt(5)) {
                case 0 -> {
                    cursor = random.nextInt(expected.length() + 1);
                    buffer.moveTo(cursor);
                }
                case 1, 2 -> {
                    String text = randomText(random, 1 + random.nextInt(6));
                    buffer.insert(text);
                    expected.insert(cursor, text);
                    cursor += text.length();
                }
                case 3 -> {
                    int count = random.nextInt(5);
                    int removed = Math.min(count, cursor);
                    assertEquals(removed, buffer.deleteBackward(count));
                    expected.delete(cursor - removed, cursor);
                    cursor -= removed;
                }
                default -> {
                    int count = random.nextInt(5);
                    int removed = Math.min(count, expected.length() - cursor);
                    assertEquals(removed, buffer.deleteForward(count));
                    expected.delete(cursor, cursor + removed);
                }
            }
            assertEquals(cursor, buffer.cursor(), "cursor after step " + step);
            assertEquals(expected.length(), buffer.length(), "length after step " + step);
            if (expected.length() > 0) {
                int i = random.nextInt(expected.length());
                assertEquals(expected.charAt(i), buffer.charAt(i), "charAt(" + i + ") after step " + step);
            }
            if (step % 500 == 0) {
                assertEquals(expected.toString(), buffer.text(), "text after step " + step);
                int from = random.nextInt(expected.length() + 1);
                int to = from + random.nextInt(expected.length() - from + 1);
                assertEquals(expected.substring(from, to), buffer.substring(from, to));
            }
        }
        assertEquals(expected.toString(), buffer.text());
    }

    @Test
    void typingInTheMiddleOfALargeTextIsFast() {
        String head = "x".repeat(150_000);
        var buffer = new TextBuffer(head + "x".repeat(2_000_000));
        buffer.moveTo(150_000);
        assertTimeoutPreemptively(Duration.ofSeconds(4), () -> {
            for (int i = 0; i < 300_000; i++) {
                buffer.insert(i % 50 == 49 ? "\n" : "a");
                if (i % 1_000 == 999) {
                    buffer.deleteBackward(10);
                    buffer.insert("0123456789");
                }
            }
        }, "300,000 keystrokes before 2 MB of text: is something shifting the whole tail?");
        assertEquals(2_450_000, buffer.length());
        assertEquals(450_000, buffer.cursor());
        assertEquals('x', buffer.charAt(149_999));
        assertEquals('a', buffer.charAt(150_000));
        assertEquals('\n', buffer.charAt(150_049));
        assertEquals("0123456789", buffer.substring(150_990, 151_000));
        assertEquals('x', buffer.charAt(2_449_999));
    }

    @Test
    void movingTheCursorCostsOnlyTheDistance() {
        var buffer = new TextBuffer("y".repeat(600_000));
        var random = new Random(11);
        assertTimeoutPreemptively(Duration.ofSeconds(4), () -> {
            int position = 300_000;
            for (int jump = 0; jump < 2_000; jump++) {
                position = Math.max(0, Math.min(buffer.length(), position + random.nextInt(20_001) - 10_000));
                buffer.moveTo(position);
                for (int i = 0; i < 20; i++) {
                    buffer.insert("z");
                }
                buffer.deleteBackward(20);
                for (int i = 0; i < 20_000; i++) {
                    buffer.moveTo(position + (i & 1));
                }
            }
            long sum = 0;
            for (int i = 0; i < buffer.length(); i++) {
                sum += buffer.charAt(i);
            }
            assertEquals(600_000L * 'y', sum);
        }, "a short move of the cursor must cost only the distance");
        assertEquals("y".repeat(600_000), buffer.text());
    }

    private static String randomText(Random random, int length) {
        var out = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int r = random.nextInt(30);
            out.append(r < 26 ? (char) ('a' + r) : r < 28 ? ' ' : '\n');
        }
        return out.toString();
    }
}
