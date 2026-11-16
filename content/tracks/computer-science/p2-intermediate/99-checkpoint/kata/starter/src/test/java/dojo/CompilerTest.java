package dojo;

import static dojo.Op.ADD;
import static dojo.Op.MUL;
import static dojo.Op.PRINT;
import static dojo.Op.PUSH;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class CompilerTest {
    @Test
    void multiplication_is_emitted_before_addition() {
        assertEquals(
                List.of(Instr.of(PUSH, 1), Instr.of(PUSH, 2), Instr.of(PUSH, 3), Instr.of(MUL), Instr.of(ADD), Instr.of(PRINT)),
                Compiler.compile("print 1 + 2 * 3"));
    }

    @Test
    void a_loop_prints_its_counter() {
        String program = """
                let i = 0
                while i < 3 {
                    print i
                    i = i + 1
                }
                """;
        assertEquals(List.of(0L, 1L, 2L), Vm.run(Compiler.compile(program)));
    }
}
