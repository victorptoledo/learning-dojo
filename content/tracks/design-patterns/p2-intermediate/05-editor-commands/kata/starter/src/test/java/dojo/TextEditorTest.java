package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Characterisation tests: they pin today's behaviour of TextEditor, so they pass before and
 * after the refactoring. Keep them green while you move the code.
 */
class TextEditorTest {
    @Test
    void edits_change_the_text_and_the_history() {
        var editor = new TextEditor();
        editor.insert(0, "hello world");
        editor.delete(5, 6);
        editor.insert(5, ", dojo");
        editor.replaceAll("o", "0");
        assertEquals("hell0, d0j0", editor.text());
        assertEquals(List.of("insert 11", "delete 6", "insert 6", "replace o->0"), editor.history());
    }

    @Test
    void undo_goes_back_one_edit_at_a_time() {
        var editor = new TextEditor();
        editor.insert(0, "abc");
        editor.delete(0, 1);
        editor.undo();
        assertEquals("abc", editor.text());
        editor.undo();
        assertEquals("", editor.text());
        assertThrows(IllegalStateException.class, editor::undo);
    }

    @Test
    void edits_out_of_range_are_refused_and_not_recorded() {
        var editor = new TextEditor();
        editor.insert(0, "abc");
        assertThrows(IndexOutOfBoundsException.class, () -> editor.insert(4, "x"));
        assertThrows(IndexOutOfBoundsException.class, () -> editor.delete(2, 5));
        assertEquals(List.of("insert 3"), editor.history());
    }
}
