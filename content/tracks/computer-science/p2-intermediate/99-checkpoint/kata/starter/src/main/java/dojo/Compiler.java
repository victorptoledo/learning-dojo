package dojo;

import java.util.List;

/**
 * Compiles a small imperative language to stack-machine bytecode.
 *
 * <p>Suggested shape: a lexer that produces tokens with line numbers, then a recursive-descent
 * parser with one method per grammar rule that emits instructions as it parses (no AST needed).
 */
public final class Compiler {
    private Compiler() {}

    public static List<Instr> compile(String source) {
        throw new UnsupportedOperationException("todo: compile " + source.length() + " characters");
    }
}
