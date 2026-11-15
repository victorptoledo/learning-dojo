package dojo;

import static dojo.GradingSupport.code;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class EditorGradingTest {
    /** A command the grader invents: your Editor must run it without knowing it. */
    static final class Shout implements Command {
        private String before;

        @Override
        public void execute(Document document) {
            before = document.text();
            document.replaceWith(before.toUpperCase(Locale.ROOT));
        }

        @Override
        public void undo(Document document) {
            document.replaceWith(before);
        }

        @Override
        public String describe() {
            return "shout";
        }
    }

    @Test
    void the_old_api_still_works_and_gains_redo() {
        var editor = new TextEditor();
        editor.insert(0, "one two");
        editor.replaceAll("o", "*");
        editor.undo();
        assertEquals("one two", editor.text());
        editor.redo();
        assertEquals("*ne tw*", editor.text());
        assertEquals(List.of("insert 7", "replace o->*"), editor.history());
        assertThrows(IllegalStateException.class, editor::redo);
    }

    @Test
    void undo_and_redo_walk_the_history_both_ways() {
        var editor = new Editor();
        editor.run(new Insert(0, "abcdef"));
        editor.run(new Delete(1, 2));
        editor.run(new Insert(0, ">"));
        assertEquals(">adef", editor.text());
        editor.undo();
        editor.undo();
        assertEquals("abcdef", editor.text());
        assertEquals(List.of("insert 6"), editor.history());
        editor.redo();
        assertEquals("adef", editor.text());
        editor.redo();
        assertEquals(">adef", editor.text());
        var error = assertThrows(IllegalStateException.class, editor::redo);
        assertEquals("nothing to redo", error.getMessage());
    }

    @Test
    void a_new_command_clears_the_redo_stack() {
        var editor = new Editor();
        editor.run(new Insert(0, "abc"));
        editor.undo();
        editor.run(new Insert(0, "xyz"));
        assertThrows(IllegalStateException.class, editor::redo);
        assertEquals("xyz", editor.text());
        editor.undo();
        assertEquals("", editor.text());
        var error = assertThrows(IllegalStateException.class, editor::undo);
        assertEquals("nothing to undo", error.getMessage());
    }

    @Test
    void delete_puts_back_exactly_what_it_removed() {
        var editor = new Editor();
        editor.run(new Insert(0, "keep THIS keep"));
        editor.run(new Delete(5, 5));
        assertEquals("keep keep", editor.text());
        editor.undo();
        assertEquals("keep THIS keep", editor.text());
    }

    @Test
    void a_command_that_fails_is_not_recorded() {
        var editor = new Editor();
        editor.run(new Insert(0, "abc"));
        assertThrows(IndexOutOfBoundsException.class, () -> editor.run(new Delete(1, 9)));
        assertThrows(IndexOutOfBoundsException.class, () -> editor.run(new Insert(-1, "x")));
        assertEquals("abc", editor.text());
        assertEquals(List.of("insert 3"), editor.history());
    }

    @Test
    void the_editor_runs_commands_it_has_never_heard_of() {
        var editor = new Editor();
        editor.run(new Insert(0, "quiet"));
        editor.run(new Shout());
        assertEquals("QUIET", editor.text());
        assertEquals(List.of("insert 5", "shout"), editor.history());
        editor.undo();
        assertEquals("quiet", editor.text());
    }

    @Test
    void a_macro_runs_and_undoes_as_one_step() {
        var editor = new Editor();
        editor.run(new Insert(0, "draft"));
        editor.run(new Macro("headline", List.of(new Insert(0, "# "), new Shout(), new Insert(7, "!"))));
        assertEquals("# DRAFT!", editor.text());
        assertEquals(List.of("insert 5", "headline"), editor.history());
        editor.undo();
        assertEquals("draft", editor.text());
        editor.redo();
        assertEquals("# DRAFT!", editor.text());
    }

    @Test
    void a_macro_that_fails_halfway_leaves_no_trace() {
        var editor = new Editor();
        editor.run(new Insert(0, "abc"));
        var broken = new Macro("broken", List.of(new Insert(0, "x"), new Shout(), new Delete(10, 1)));
        assertThrows(IndexOutOfBoundsException.class, () -> editor.run(broken));
        assertEquals("abc", editor.text());
        assertEquals(List.of("insert 3"), editor.history());
    }

    @Test
    void commands_undo_themselves_and_the_editor_keeps_no_copies_of_the_text() {
        assertTrue(Command.class.isInterface(), "Command must be an interface");
        for (Class<?> type : List.of(Insert.class, Delete.class, ReplaceAll.class, Macro.class)) {
            assertTrue(Command.class.isAssignableFrom(type), type.getSimpleName() + " must implement Command");
        }
        List<Field> fields = Arrays.stream(Editor.class.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .toList();
        for (Field field : fields) {
            String type = field.getGenericType().getTypeName();
            assertFalse(type.contains("String"), "Editor must not keep text snapshots; found field " + field.getName());
        }
        String code = code("dojo.Editor");
        for (String forbidden : List.of("instanceof", "switch", "toString()")) {
            assertFalse(code.contains(forbidden), "Editor should only run, undo and redo commands; found " + forbidden);
        }
        assertFalse(code("dojo.TextEditor").contains("snapshot"), "TextEditor should delegate to Editor");
    }
}
