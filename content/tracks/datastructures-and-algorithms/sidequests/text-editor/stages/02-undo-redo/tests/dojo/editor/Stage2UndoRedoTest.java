package dojo.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class Stage2UndoRedoTest {

    private static void type(TextBuffer buffer, String text) {
        for (char c : text.toCharArray()) {
            buffer.insert(String.valueOf(c));
        }
    }

    @Test
    void nothingToUndoAtFirst() {
        var buffer = new TextBuffer("given");
        assertFalse(buffer.undo(), "the initial text is not a step");
        assertFalse(buffer.redo());
        assertEquals(0, buffer.undoSize());
        assertEquals(0, buffer.redoSize());
        assertEquals("given", buffer.text());
    }

    @Test
    void theReadmeExample() {
        var buffer = new TextBuffer();
        type(buffer, "hello world");
        assertEquals(2, buffer.undoSize(), "two words, two steps");
        assertTrue(buffer.undo());
        assertEquals("hello", buffer.text());
        assertEquals(5, buffer.cursor());
        assertEquals(1, buffer.redoSize());
        buffer.deleteBackward(1);
        assertEquals("hell", buffer.text());
        assertEquals(0, buffer.redoSize(), "a new edit clears redo");
        assertTrue(buffer.undo());
        assertEquals("hello", buffer.text());
        assertTrue(buffer.undo());
        assertEquals("", buffer.text());
        assertFalse(buffer.undo());
        assertTrue(buffer.redo());
        assertEquals("hello", buffer.text());
        assertEquals(5, buffer.cursor());
    }

    @Test
    void typingIsGroupedByWords() {
        var buffer = new TextBuffer();
        type(buffer, "one two  three\nfour");
        // "one", " two", "  three", "\nfour"
        assertEquals(4, buffer.undoSize());
        buffer.undo();
        assertEquals("one two  three", buffer.text());
        buffer.undo();
        assertEquals("one two", buffer.text());
        buffer.undo();
        assertEquals("one", buffer.text());
        buffer.undo();
        assertEquals("", buffer.text());
    }

    @Test
    void aSpaceAfterASpaceStaysInTheSameStep() {
        var buffer = new TextBuffer();
        type(buffer, "a   ");
        buffer.insert(" b");
        assertEquals(2, buffer.undoSize());
        buffer.undo();
        assertEquals("a", buffer.text());
    }

    @Test
    void movingTheCursorClosesTheStep() {
        var buffer = new TextBuffer("ab");
        type(buffer, "cd");
        buffer.moveTo(buffer.cursor());
        type(buffer, "ef");
        assertEquals(1, buffer.undoSize(), "a move to the same place changes nothing");
        buffer.moveTo(0);
        type(buffer, "xy");
        assertEquals("xyabcdef", buffer.text());
        assertEquals(2, buffer.undoSize());
        buffer.undo();
        assertEquals("abcdef", buffer.text());
        assertEquals(0, buffer.cursor(), "the cursor goes back to where the step began");
        buffer.undo();
        assertEquals("ab", buffer.text());
        assertEquals(2, buffer.cursor());
    }

    @Test
    void backspacesAndDeletesAreGroupedByKind() {
        var buffer = new TextBuffer("0123456789");
        buffer.moveTo(6);
        buffer.deleteBackward(1);
        buffer.deleteBackward(2);
        assertEquals("0126789", buffer.text());
        buffer.deleteForward(1);
        buffer.deleteForward(1);
        assertEquals("01289", buffer.text());
        buffer.deleteBackward(0);
        buffer.insert("");
        assertEquals(2, buffer.undoSize(), "calls that change nothing are not edits");
        buffer.undo();
        assertEquals("0126789", buffer.text());
        assertEquals(3, buffer.cursor());
        buffer.undo();
        assertEquals("0123456789", buffer.text());
        assertEquals(6, buffer.cursor());
        buffer.redo();
        assertEquals("0126789", buffer.text());
        assertEquals(3, buffer.cursor());
        buffer.redo();
        assertEquals("01289", buffer.text());
        assertEquals(3, buffer.cursor());
        assertFalse(buffer.redo());
    }

    @Test
    void anInsertAfterADeleteIsANewStep() {
        var buffer = new TextBuffer("cat");
        buffer.deleteBackward(1);
        type(buffer, "r");
        type(buffer, "d");
        assertEquals("card", buffer.text());
        assertEquals(2, buffer.undoSize());
        buffer.undo();
        assertEquals("ca", buffer.text());
        buffer.undo();
        assertEquals("cat", buffer.text());
        assertEquals(3, buffer.cursor());
    }

    @Test
    void undoAndRedoCloseTheStep() {
        var buffer = new TextBuffer();
        type(buffer, "ab");
        buffer.undo();
        buffer.redo();
        type(buffer, "cd");
        assertEquals(2, buffer.undoSize(), "typing after a redo starts a new step");
        buffer.undo();
        assertEquals("ab", buffer.text());
    }

    @Test
    void randomSessionsUndoBackToEveryEarlierText() {
        var random = new Random(3);
        for (int session = 0; session < 30; session++) {
            var buffer = new TextBuffer("start here");
            List<String> texts = new ArrayList<>();
            List<Integer> cursors = new ArrayList<>();
            texts.add(buffer.text());
            cursors.add(buffer.cursor());
            for (int step = 0; step < 300; step++) {
                // closing the step by a move makes every edit its own step, so the history is known
                buffer.moveTo(random.nextInt(buffer.length() + 1));
                buffer.moveTo(buffer.cursor() == 0 ? 1 % (buffer.length() + 1) : buffer.cursor() - 1);
                int sizeBefore = buffer.undoSize();
                int cursorBefore = buffer.cursor();
                switch (buffer.length() < 5 ? 0 : random.nextInt(3)) {
                    case 0 -> buffer.insert("w" + random.nextInt(100));
                    case 1 -> buffer.deleteBackward(1 + random.nextInt(3));
                    default -> buffer.deleteForward(1 + random.nextInt(3));
                }
                if (buffer.undoSize() > sizeBefore) {
                    texts.add(buffer.text());
                    cursors.add(cursorBefore);
                }
            }
            for (int i = texts.size() - 1; i > 0; i--) {
                assertEquals(texts.get(i), buffer.text());
                assertTrue(buffer.undo());
                assertEquals(texts.get(i - 1), buffer.text(), "session " + session + ", undo to " + (i - 1));
                assertEquals((int) cursors.get(i), buffer.cursor(), "cursor after undo to " + (i - 1));
            }
            assertFalse(buffer.undo());
            for (int i = 1; i < texts.size(); i++) {
                assertTrue(buffer.redo());
                assertEquals(texts.get(i), buffer.text(), "session " + session + ", redo to " + i);
            }
            assertFalse(buffer.redo());
        }
    }

    @Test
    void fiftyThousandStepsOnALargeTextFitInMemoryAndTime() {
        String initial = "q".repeat(500_000);
        var random = new Random(5);
        assertTimeoutPreemptively(Duration.ofSeconds(6), () -> {
            try {
                editUndoAndRedo(initial, random);
            } catch (OutOfMemoryError e) {
                // when this thread is the one that runs out (the JVM may die first), say why
                throw new AssertionError("50,000 steps ran out of a 256 MB heap: does a step copy the whole text?");
            }
        }, "50,000 steps undone and redone: does a step copy the whole text?");
    }

    private static void editUndoAndRedo(String initial, Random random) {
        var buffer = new TextBuffer(initial);
        int position = 0;
        for (int step = 0; step < 50_000; step++) {
            // a person editing down the file: every step a little further on
            position = (position + 3 + random.nextInt(200)) % buffer.length();
            buffer.moveTo(position);
            if (step % 3 == 2) {
                buffer.deleteForward(2);
            } else {
                buffer.insert("ab");
            }
        }
        String edited = buffer.text();
        assertEquals(50_000, buffer.undoSize());
        while (buffer.undo()) {
            // all the way back
        }
        assertEquals(initial, buffer.text());
        while (buffer.redo()) {
            // and forward again
        }
        assertEquals(edited, buffer.text());
    }
}
