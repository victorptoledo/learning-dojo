package dojo;

import static dojo.Op.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CompilerGradingTest {

    private static Instr i(Op op) {
        return Instr.of(op);
    }

    private static Instr i(Op op, long arg) {
        return Instr.of(op, arg);
    }

    private static List<Long> out(String program) {
        return Vm.run(Compiler.compile(program));
    }

    // ---- bytecode -------------------------------------------------------------------------

    @Test
    void expressions_compile_in_post_order() {
        assertEquals(
                List.of(i(PUSH, 7), i(PUSH, 2), i(SUB), i(PUSH, 1), i(SUB), i(PRINT)),
                Compiler.compile("print 7 - 2 - 1"));
        assertEquals(
                List.of(i(PUSH, 1), i(PUSH, 2), i(ADD), i(PUSH, 3), i(MUL), i(PRINT)),
                Compiler.compile("print (1 + 2) * 3"));
        assertEquals(
                List.of(i(PUSH, 4), i(NEG), i(NEG), i(PUSH, 3), i(MOD), i(PRINT)),
                Compiler.compile("print --4 % 3"));
    }

    @Test
    void variables_get_slots_in_declaration_order() {
        assertEquals(
                List.of(i(PUSH, 5), i(STORE, 0), i(PUSH, 6), i(STORE, 1),
                        i(LOAD, 1), i(LOAD, 0), i(SUB), i(STORE, 0), i(LOAD, 0), i(PRINT)),
                Compiler.compile("let x = 5; let y = 6\nx = y - x; print x"));
    }

    @Test
    void comparisons_compile_to_one_instruction_each() {
        assertEquals(
                List.of(i(LOAD, 0), i(PUSH, 3), i(LE), i(PRINT)),
                Compiler.compile("let a = 0; print a <= 3").subList(2, 6));
        String[][] cases = {{"<", "LT"}, {"<=", "LE"}, {">", "GT"}, {">=", "GE"}, {"==", "EQ"}, {"!=", "NE"}};
        for (String[] c : cases) {
            assertEquals(Op.valueOf(c[1]), Compiler.compile("print 1 " + c[0] + " 2").get(2).op(), c[0]);
        }
    }

    @Test
    void while_compiles_to_a_conditional_exit_and_a_backward_jump() {
        String program = """
                let i = 0
                while i < 3 {
                    print i
                    i = i + 1
                }
                """;
        assertEquals(List.of(
                i(PUSH, 0), i(STORE, 0),
                i(LOAD, 0), i(PUSH, 3), i(LT), i(JUMP_IF_ZERO, 13),
                i(LOAD, 0), i(PRINT),
                i(LOAD, 0), i(PUSH, 1), i(ADD), i(STORE, 0),
                i(JUMP, 2)), Compiler.compile(program));
    }

    @Test
    void if_else_compiles_to_two_jumps() {
        assertEquals(List.of(
                i(PUSH, 1), i(JUMP_IF_ZERO, 5),
                i(PUSH, 10), i(PRINT), i(JUMP, 7),
                i(PUSH, 20), i(PRINT)), Compiler.compile("if 1 { print 10 } else { print 20 }"));
        assertEquals(List.of(
                i(PUSH, 0), i(JUMP_IF_ZERO, 4), i(PUSH, 10), i(PRINT), i(PUSH, 30), i(PRINT)),
                Compiler.compile("if 0 { print 10 }\nprint 30"));
    }

    @Test
    void empty_programs_and_comments_compile_to_nothing() {
        assertEquals(List.of(), Compiler.compile(""));
        assertEquals(List.of(), Compiler.compile("\n\n  # just a comment\n;;\n"));
        assertEquals(List.of(i(PUSH, 1), i(PRINT)), Compiler.compile("print 1 # one\n"));
    }

    // ---- running --------------------------------------------------------------------------

    @Test
    void arithmetic_uses_64_bit_java_semantics() {
        assertEquals(List.of(-3L, -1L, 3L, 4_000_000_000L, Long.MIN_VALUE),
                out("print -7 / 2\nprint -7 % 2\nprint 7 / 2\nprint 2000000000 * 2\n"
                        + "print 9223372036854775807 + 1"));
    }

    @Test
    void comparisons_produce_one_or_zero() {
        assertEquals(List.of(1L, 0L, 1L, 1L, 0L, 1L, 1L),
                out("print 1 < 2; print 2 < 1; print 2 <= 2; print 3 > 2; print 3 >= 4;"
                        + "print 5 == 5; print 5 != 6"));
    }

    @Test
    void else_if_chains() {
        String program = """
                let n = 0
                while n < 4 {
                    if n == 0 { print 100 } else if n == 1 { print 200 } else { print n }
                    n = n + 1
                }
                """;
        assertEquals(List.of(100L, 200L, 2L, 3L), out(program));
    }

    @Test
    void fibonacci() {
        String program = """
                let a = 0
                let b = 1
                let n = 0
                while n < 10 {
                    print a
                    let t = 0
                    t = a + b; a = b; b = t
                    n = n + 1
                }
                """;
        // `let t` inside the loop body is compiled once, so it is fine to run it many times
        assertEquals(List.of(0L, 1L, 1L, 2L, 3L, 5L, 8L, 13L, 21L, 34L), out(program));
    }

    @Test
    void euclid_gcd_and_nested_loops() {
        String gcd = """
                let a = 1071
                let b = 462
                while b != 0 { let t = b; b = a % b; a = t }
                print a
                """;
        assertEquals(List.of(21L), out(gcd));
        String primes = """
                let n = 2
                while n < 30 {
                    let d = 2
                    let prime = 1
                    while d * d <= n {
                        if n % d == 0 { prime = 0 }
                        d = d + 1
                    }
                    if prime { print n }
                    n = n + 1
                }
                """;
        assertEquals(List.of(2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L, 23L, 29L), out(primes));
    }

    // ---- errors ---------------------------------------------------------------------------

    // In these sources, ~ stands for a line break.
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "print x | 1",
            "let a = 1~print a + b | 2",
            "let a = 1~~let a = 2 | 3",
            "let x = x + 1 | 1",
            "y = 3 | 1",
            "print (1 + 2 | 1",
            "print 1 +~print 2 | 1",
            "while 1 { print 1 | 1",
            "print 1 2 | 1",
            "let = 4 | 1",
            "print 1 < 2 < 3 | 1",
            "~~print 1 & 2 | 3",
            "print 99999999999999999999 | 1",
            "let print = 1 | 1",
            "} | 1",
    })
    void compile_errors_report_the_line(String source, int line) {
        CompileError error = assertThrows(CompileError.class, () -> Compiler.compile(source.replace('~', '\n')));
        assertEquals(line, error.line(), source);
        assertTrue(error.getMessage().startsWith("line " + line + ": "), error.getMessage());
    }

    @Test
    void division_by_zero_is_a_vm_error() {
        assertThrows(VmError.class, () -> out("let z = 0\nprint 1 / z"));
        assertThrows(VmError.class, () -> out("print 5 % (2 - 2)"));
    }

    @Test
    void endless_loops_hit_the_step_limit() {
        List<Instr> code = Compiler.compile("while 1 { }");
        assertThrows(VmError.class, () -> Vm.run(code, 1_000));
        assertEquals(List.of(1L), Vm.run(Compiler.compile("print 1"), 2));
        assertThrows(VmError.class, () -> Vm.run(Compiler.compile("print 1"), 1));
    }

    @Test
    void the_vm_rejects_broken_bytecode() {
        assertThrows(VmError.class, () -> Vm.run(List.of(i(ADD))));
        assertThrows(VmError.class, () -> Vm.run(List.of(i(PUSH, 1), i(ADD))));
        assertThrows(VmError.class, () -> Vm.run(List.of(i(PRINT))));
        assertThrows(VmError.class, () -> Vm.run(List.of(i(JUMP, 5))));
        assertThrows(VmError.class, () -> Vm.run(List.of(i(PUSH, 0), i(JUMP_IF_ZERO, -1))));
        assertEquals(List.of(), Vm.run(List.of(i(JUMP, 1))));
    }

    @Test
    void handwritten_bytecode_runs() {
        // a countdown from 3, written directly in bytecode
        List<Instr> code = List.of(
                i(PUSH, 3), i(STORE, 0),
                i(LOAD, 0), i(JUMP_IF_ZERO, 11),
                i(LOAD, 0), i(PRINT),
                i(LOAD, 0), i(PUSH, 1), i(SUB), i(STORE, 0),
                i(JUMP, 2));
        assertEquals(List.of(3L, 2L, 1L), Vm.run(code));
    }

    @Test
    void random_expressions_match_java() {
        Random random = new Random(64);
        for (int n = 0; n < 500; n++) {
            StringBuilder src = new StringBuilder();
            long expected = randomExpression(random, 4, src);
            assertEquals(List.of(expected), out("print " + src), src.toString());
        }
    }

    private static long randomExpression(Random random, int depth, StringBuilder out) {
        if (depth == 0 || random.nextInt(4) == 0) {
            long value = random.nextInt(50);
            out.append(value);
            return value;
        }
        switch (random.nextInt(5)) {
            case 0 -> {
                out.append("-(");
                long v = randomExpression(random, depth - 1, out);
                out.append(")");
                return -v;
            }
            case 1 -> {
                out.append("(");
                long a = randomExpression(random, depth - 1, out);
                out.append(" + ");
                long b = randomExpression(random, depth - 1, out);
                out.append(")");
                return a + b;
            }
            case 2 -> {
                out.append("(");
                long a = randomExpression(random, depth - 1, out);
                out.append(" - ");
                long b = randomExpression(random, depth - 1, out);
                out.append(")");
                return a - b;
            }
            case 3 -> {
                out.append("(");
                long a = randomExpression(random, depth - 1, out);
                out.append(" * ");
                long b = randomExpression(random, depth - 1, out);
                out.append(")");
                return a * b;
            }
            default -> {
                out.append("(");
                long a = randomExpression(random, depth - 1, out);
                out.append(" < ");
                long b = randomExpression(random, depth - 1, out);
                out.append(")");
                return a < b ? 1 : 0;
            }
        }
    }

    @Test
    void a_long_loop_runs_quickly() {
        String program = """
                let i = 0
                let sum = 0
                while i < 1000000 { sum = sum + i % 7; i = i + 1 }
                print sum
                """;
        long expected = 0;
        for (int k = 0; k < 1_000_000; k++) expected += k % 7;
        long want = expected;
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> assertEquals(List.of(want), out(program)));
    }
}
