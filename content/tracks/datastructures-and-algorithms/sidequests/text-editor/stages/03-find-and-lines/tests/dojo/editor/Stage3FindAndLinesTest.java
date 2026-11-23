package dojo.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Random;
import org.junit.jupiter.api.Test;

class Stage3FindAndLinesTest {

    @Test
    void theReadmeExample() {
        var buffer = new TextBuffer("one fish\ntwo fish\n");
        assertEquals(4, buffer.find("fish", 0));
        assertEquals(13, buffer.find("fish", 5));
        assertEquals(-1, buffer.find("fish", 14));
        assertEquals(3, buffer.lineCount());
        assertEquals(1, buffer.lineOf(13));
        assertEquals(18, buffer.lineStart(2));
        assertEquals(2, buffer.replaceAll("fish", "cat"));
        assertEquals("one cat\ntwo cat\n", buffer.text());
        assertEquals(15, buffer.cursor());
        assertTrue(buffer.undo());
        assertEquals("one fish\ntwo fish\n", buffer.text());
    }

    @Test
    void findSeesMatchesAcrossTheGapAndAtTheEdges() {
        var buffer = new TextBuffer("abcabcabd");
        buffer.moveTo(4);
        assertEquals(0, buffer.find("abc", 0));
        assertEquals(3, buffer.find("abc", 1), "the match spans the cursor");
        assertEquals(6, buffer.find("abd", 0));
        assertEquals(6, buffer.find("abd", 6));
        assertEquals(-1, buffer.find("abd", 7));
        assertEquals(-1, buffer.find("abcabcabdx", 0));
        assertEquals(-1, buffer.find("a", 9), "from may be the length");
        assertEquals(0, buffer.find("abcabcabd", 0));
        assertEquals(4, buffer.cursor(), "find does not move the cursor");
    }

    @Test
    void findHandlesPatternsThatOverlapThemselves() {
        var buffer = new TextBuffer("aabaabaaab");
        assertEquals(6, buffer.find("aaab", 0));
        assertEquals(0, buffer.find("aabaab", 0));
        assertEquals(3, buffer.find("aabaa", 1));
        var other = new TextBuffer("abababca");
        assertEquals(2, other.find("ababc", 0));
    }

    @Test
    void badSearchesAreRefused() {
        var buffer = new TextBuffer("abc");
        assertThrows(IllegalArgumentException.class, () -> buffer.find("", 0));
        assertThrows(IllegalArgumentException.class, () -> buffer.find(null, 0));
        assertThrows(IllegalArgumentException.class, () -> buffer.find("a", -1));
        assertThrows(IllegalArgumentException.class, () -> buffer.find("a", 4));
        assertThrows(IllegalArgumentException.class, () -> buffer.replaceAll("", "x"));
        assertThrows(IllegalArgumentException.class, () -> buffer.replaceAll("a", null));
        assertThrows(IllegalArgumentException.class, () -> buffer.lineOf(4));
        assertThrows(IllegalArgumentException.class, () -> buffer.lineOf(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.lineStart(1));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.lineStart(-1));
    }

    @Test
    void randomFindsMatchIndexOf() {
        var random = new Random(13);
        for (int round = 0; round < 300; round++) {
            var text = new StringBuilder();
            int length = random.nextInt(200);
            for (int i = 0; i < length; i++) {
                text.append((char) ('a' + random.nextInt(2)));
            }
            var buffer = new TextBuffer(text.toString());
            buffer.moveTo(random.nextInt(length + 1));
            for (int query = 0; query < 20; query++) {
                var pattern = new StringBuilder();
                int m = 1 + random.nextInt(6);
                for (int i = 0; i < m; i++) {
                    pattern.append((char) ('a' + random.nextInt(2)));
                }
                int from = random.nextInt(length + 1);
                assertEquals(text.indexOf(pattern.toString(), from), buffer.find(pattern.toString(), from),
                        "find '" + pattern + "' from " + from + " in '" + text + "'");
            }
        }
    }

    @Test
    void replaceAllGoesLeftToRightWithoutOverlaps() {
        var buffer = new TextBuffer("aaaa");
        assertEquals(2, buffer.replaceAll("aa", "b"));
        assertEquals("bb", buffer.text());
        var growing = new TextBuffer("x.x.x");
        assertEquals(3, growing.replaceAll("x", "xx"));
        assertEquals("xx.xx.xx", growing.text(), "a replacement is never searched again");
        var removing = new TextBuffer("a, b, c");
        assertEquals(2, removing.replaceAll(", ", ""));
        assertEquals("abc", removing.text());
        assertEquals(2, removing.cursor(), "the cursor ends after the last replacement, between b and c");
    }

    @Test
    void replaceAllIsOneStep() {
        var buffer = new TextBuffer("red green red blue red");
        buffer.moveTo(4);
        buffer.insert("dark ");
        assertEquals(3, buffer.replaceAll("red", "pink"));
        assertEquals("pink dark green pink blue pink", buffer.text());
        assertEquals(2, buffer.undoSize());
        assertTrue(buffer.undo());
        assertEquals("red dark green red blue red", buffer.text());
        assertEquals(9, buffer.cursor(), "the cursor goes back to where it was");
        assertTrue(buffer.redo());
        assertEquals("pink dark green pink blue pink", buffer.text());
        assertEquals(30, buffer.cursor());
        assertTrue(buffer.undo());
        assertTrue(buffer.undo());
        assertEquals("red green red blue red", buffer.text());
    }

    @Test
    void replaceAllWithoutMatchesChangesNothing() {
        var buffer = new TextBuffer("abc");
        buffer.insert("d");
        buffer.undo();
        assertEquals(0, buffer.replaceAll("zz", "y"));
        assertEquals("abc", buffer.text());
        assertEquals(0, buffer.undoSize());
        assertEquals(1, buffer.redoSize(), "redo keeps what it had");
        assertTrue(buffer.redo());
        assertEquals("abcd", buffer.text());
    }

    @Test
    void typingAfterAReplaceIsANewStep() {
        var buffer = new TextBuffer("cat");
        buffer.replaceAll("cat", "dog");
        buffer.insert("s");
        assertEquals("dogs", buffer.text());
        buffer.undo();
        assertEquals("dog", buffer.text());
        buffer.undo();
        assertEquals("cat", buffer.text());
        assertFalse(buffer.undo());
    }

    @Test
    void linesFollowEveryChange() {
        var buffer = new TextBuffer("");
        assertEquals(1, buffer.lineCount());
        assertEquals(0, buffer.lineOf(0));
        buffer.insert("a\nbb\n");
        assertEquals(3, buffer.lineCount());
        assertEquals(0, buffer.lineStart(0));
        assertEquals(2, buffer.lineStart(1));
        assertEquals(5, buffer.lineStart(2));
        assertEquals(0, buffer.lineOf(1), "the line break belongs to the line it ends");
        assertEquals(1, buffer.lineOf(2));
        assertEquals(2, buffer.lineOf(5));
        buffer.moveTo(1);
        buffer.deleteForward(1);
        assertEquals(2, buffer.lineCount());
        assertEquals(0, buffer.lineOf(2));
        buffer.undo();
        assertEquals(3, buffer.lineCount());
        buffer.replaceAll("\n", "\n\n");
        assertEquals(5, buffer.lineCount());
        assertEquals(7, buffer.lineStart(4));
        buffer.undo();
        assertEquals(3, buffer.lineCount());
    }

    @Test
    void searchingALargeTextIsLinear() {
        String text = "a".repeat(1_000_000);
        var buffer = new TextBuffer(text);
        buffer.moveTo(500_000);
        String pattern = "a".repeat(5_000) + "b";
        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            assertEquals(-1, buffer.find(pattern, 0));
            assertEquals(-1, buffer.find(pattern, 1));
        }, "aaaa...ab in aaaa...a: a naive search compares 5,000 characters at every position");
    }

    @Test
    void findNextDoesNotCopyTheText() {
        var document = new StringBuilder();
        for (int i = 0; i < 1_000; i++) {
            document.append("x".repeat(990)).append("needle\n123");
        }
        var buffer = new TextBuffer(document.toString());
        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            int found = 0;
            for (int round = 0; round < 10; round++) {
                int at = buffer.find("needle", 0);
                while (at >= 0) {
                    found++;
                    at = buffer.find("needle", at + 1);
                }
            }
            assertEquals(10_000, found);
        }, "find next 10,000 times on a megabyte: is each search copying the buffer?");
    }

    @Test
    void replacingThousandsOfMatchesIsFast() {
        var document = new StringBuilder();
        for (int i = 0; i < 100_000; i++) {
            document.append("foo bar ");
        }
        var buffer = new TextBuffer(document.toString());
        assertTimeoutPreemptively(Duration.ofSeconds(4), () -> {
            assertEquals(100_000, buffer.replaceAll("foo", "quux"));
            assertEquals(900_000, buffer.length());
            assertTrue(buffer.undo());
            assertEquals(800_000, buffer.length());
            assertTrue(buffer.redo());
        });
        assertEquals("quux bar quux", buffer.substring(0, 13));
    }

    @Test
    void manyLineQuestionsAreAnsweredFromAnIndex() {
        var document = new StringBuilder();
        for (int i = 0; i < 50_000; i++) {
            document.append("line ").append(i).append('\n');
        }
        var buffer = new TextBuffer(document.toString());
        var random = new Random(17);
        assertTimeoutPreemptively(Duration.ofSeconds(4), () -> {
            assertEquals(50_001, buffer.lineCount());
            long checksum = 0;
            for (int i = 0; i < 200_000; i++) {
                int line = buffer.lineOf(random.nextInt(buffer.length() + 1));
                checksum += buffer.lineStart(line);
            }
            assertTrue(checksum > 0);
            for (int edit = 0; edit < 100; edit++) {
                buffer.moveTo(buffer.lineStart(random.nextInt(buffer.lineCount())));
                buffer.insert("new\n");
                for (int i = 0; i < 1_000; i++) {
                    buffer.lineOf(random.nextInt(buffer.length() + 1));
                }
            }
            assertEquals(50_101, buffer.lineCount());
        }, "200,000 line questions: is each one counting line breaks from the top?");
        int line = buffer.lineOf(buffer.find("line 1234\n", 0));
        assertEquals("line 1234", buffer.substring(buffer.lineStart(line), buffer.lineStart(line + 1) - 1));
    }
}
