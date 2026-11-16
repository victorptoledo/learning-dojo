package dojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Compiles a small imperative language to stack-machine bytecode in one pass over the source. */
public final class Compiler {

    private enum Kind { NUMBER, IDENT, KEYWORD, SYMBOL, NEWLINE, EOF }

    private record Token(Kind kind, String text, int line) {}

    private static final Set<String> KEYWORDS = Set.of("let", "print", "while", "if", "else");
    private static final List<String> SYMBOLS =
            List.of("<=", ">=", "==", "!=", "+", "-", "*", "/", "%", "<", ">", "=", "(", ")", "{", "}", ";");
    private static final Map<String, Op> COMPARISONS = Map.of(
            "<", Op.LT, "<=", Op.LE, ">", Op.GT, ">=", Op.GE, "==", Op.EQ, "!=", Op.NE);

    private final List<Token> tokens;
    private final List<Instr> code = new ArrayList<>();
    private final Map<String, Integer> slots = new HashMap<>();
    private int pos;

    private Compiler(String source) {
        this.tokens = tokenize(source);
    }

    public static List<Instr> compile(String source) {
        Compiler compiler = new Compiler(source);
        compiler.statements(false);
        return List.copyOf(compiler.code);
    }

    // ---- lexer ----------------------------------------------------------------------------

    private static List<Token> tokenize(String source) {
        List<Token> out = new ArrayList<>();
        int line = 1;
        int i = 0;
        outer:
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == '\n') {
                out.add(new Token(Kind.NEWLINE, "\n", line++));
                i++;
            } else if (c == ' ' || c == '\t' || c == '\r') {
                i++;
            } else if (c == '#') {
                while (i < source.length() && source.charAt(i) != '\n') i++;
            } else if (c >= '0' && c <= '9') {
                int start = i;
                while (i < source.length() && Character.isDigit(source.charAt(i)) && source.charAt(i) < 128) i++;
                String digits = source.substring(start, i);
                try {
                    Long.parseLong(digits);
                } catch (NumberFormatException e) {
                    throw new CompileError("number too large: " + digits, line);
                }
                out.add(new Token(Kind.NUMBER, digits, line));
            } else if (Character.isLetter(c) && c < 128 || c == '_') {
                int start = i;
                while (i < source.length()) {
                    char d = source.charAt(i);
                    if (d < 128 && (Character.isLetterOrDigit(d) || d == '_')) i++;
                    else break;
                }
                String word = source.substring(start, i);
                out.add(new Token(KEYWORDS.contains(word) ? Kind.KEYWORD : Kind.IDENT, word, line));
            } else {
                for (String symbol : SYMBOLS) {
                    if (source.startsWith(symbol, i)) {
                        out.add(new Token(Kind.SYMBOL, symbol, line));
                        i += symbol.length();
                        continue outer;
                    }
                }
                throw new CompileError("unexpected character '" + c + "'", line);
            }
        }
        out.add(new Token(Kind.EOF, "", line));
        return out;
    }

    // ---- helpers --------------------------------------------------------------------------

    private Token peek() {
        return tokens.get(pos);
    }

    private Token advance() {
        return tokens.get(pos++);
    }

    private boolean at(String text) {
        Token t = peek();
        return (t.kind() == Kind.SYMBOL || t.kind() == Kind.KEYWORD) && t.text().equals(text);
    }

    private Token expect(String text) {
        if (!at(text)) throw unexpected("'" + text + "'");
        return advance();
    }

    private CompileError unexpected(String wanted) {
        Token t = peek();
        String found = switch (t.kind()) {
            case EOF -> "end of input";
            case NEWLINE -> "end of line";
            default -> "'" + t.text() + "'";
        };
        return new CompileError("expected " + wanted + " but found " + found, t.line());
    }

    private int emit(Op op, long arg) {
        code.add(Instr.of(op, arg));
        return code.size() - 1;
    }

    private int emit(Op op) {
        return emit(op, 0);
    }

    private void patch(int at, int target) {
        code.set(at, Instr.of(code.get(at).op(), target));
    }

    private void skipSeparators() {
        while (peek().kind() == Kind.NEWLINE || at(";")) advance();
    }

    // ---- statements -----------------------------------------------------------------------

    /** Statements until end of input, or until a closing brace when inside a block. */
    private void statements(boolean inBlock) {
        skipSeparators();
        while (peek().kind() != Kind.EOF && !(inBlock && at("}"))) {
            statement();
            if (!(peek().kind() == Kind.NEWLINE || at(";") || at("}") || peek().kind() == Kind.EOF)) {
                throw unexpected("end of statement");
            }
            skipSeparators();
        }
        if (inBlock && !at("}")) throw unexpected("'}'");
    }

    private void block() {
        expect("{");
        statements(true);
        expect("}");
    }

    private void statement() {
        Token t = peek();
        if (at("let")) {
            advance();
            Token name = identifier();
            if (slots.containsKey(name.text())) {
                throw new CompileError("variable '" + name.text() + "' is already declared", name.line());
            }
            expect("=");
            expression();
            int slot = slots.size();
            slots.put(name.text(), slot);
            emit(Op.STORE, slot);
        } else if (at("print")) {
            advance();
            expression();
            emit(Op.PRINT);
        } else if (at("while")) {
            advance();
            int start = code.size();
            expression();
            int exit = emit(Op.JUMP_IF_ZERO);
            block();
            emit(Op.JUMP, start);
            patch(exit, code.size());
        } else if (at("if")) {
            advance();
            expression();
            int skip = emit(Op.JUMP_IF_ZERO);
            block();
            if (at("else")) {
                advance();
                int end = emit(Op.JUMP);
                patch(skip, code.size());
                if (at("if")) statement();
                else block();
                patch(end, code.size());
            } else {
                patch(skip, code.size());
            }
        } else if (t.kind() == Kind.IDENT) {
            Token name = advance();
            expect("=");
            expression();
            emit(Op.STORE, slotOf(name));
        } else {
            throw unexpected("a statement");
        }
    }

    private Token identifier() {
        if (peek().kind() != Kind.IDENT) throw unexpected("a variable name");
        return advance();
    }

    private int slotOf(Token name) {
        Integer slot = slots.get(name.text());
        if (slot == null) throw new CompileError("undefined variable '" + name.text() + "'", name.line());
        return slot;
    }

    // ---- expressions ----------------------------------------------------------------------

    private void expression() {
        sum();
        Token t = peek();
        if (t.kind() == Kind.SYMBOL && COMPARISONS.containsKey(t.text())) {
            advance();
            sum();
            emit(COMPARISONS.get(t.text()));
            Token next = peek();
            if (next.kind() == Kind.SYMBOL && COMPARISONS.containsKey(next.text())) {
                throw new CompileError("comparisons cannot be chained", next.line());
            }
        }
    }

    private void sum() {
        product();
        while (at("+") || at("-")) {
            Op op = advance().text().equals("+") ? Op.ADD : Op.SUB;
            product();
            emit(op);
        }
    }

    private void product() {
        unary();
        while (at("*") || at("/") || at("%")) {
            Op op = switch (advance().text()) {
                case "*" -> Op.MUL;
                case "/" -> Op.DIV;
                default -> Op.MOD;
            };
            unary();
            emit(op);
        }
    }

    private void unary() {
        if (at("-")) {
            advance();
            unary();
            emit(Op.NEG);
        } else {
            atom();
        }
    }

    private void atom() {
        Token t = peek();
        if (t.kind() == Kind.NUMBER) {
            advance();
            emit(Op.PUSH, Long.parseLong(t.text()));
        } else if (t.kind() == Kind.IDENT) {
            advance();
            emit(Op.LOAD, slotOf(t));
        } else if (at("(")) {
            advance();
            expression();
            expect(")");
        } else {
            throw unexpected("a number, a variable or '('");
        }
    }
}
