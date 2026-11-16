package dojo;

import java.util.ArrayList;
import java.util.List;

/** Runs stack-machine bytecode and returns what it printed. */
public final class Vm {
    public static final long DEFAULT_MAX_STEPS = 100_000_000;

    private Vm() {}

    public static List<Long> run(List<Instr> code) {
        return run(code, DEFAULT_MAX_STEPS);
    }

    public static List<Long> run(List<Instr> code, long maxSteps) {
        int slotCount = 0;
        for (Instr i : code) {
            if (i.op() == Op.LOAD || i.op() == Op.STORE) {
                if (i.arg() < 0 || i.arg() > 65_535) throw new VmError("bad slot " + i.arg());
                slotCount = Math.max(slotCount, (int) i.arg() + 1);
            }
        }
        long[] slots = new long[slotCount];
        long[] stack = new long[256];
        int sp = 0;
        List<Long> output = new ArrayList<>();
        int pc = 0;
        long steps = 0;
        while (pc != code.size()) {
            if (pc < 0 || pc > code.size()) throw new VmError("jump outside the program: " + pc);
            if (steps++ == maxSteps) throw new VmError("step limit of " + maxSteps + " reached");
            Instr instr = code.get(pc++);
            switch (instr.op()) {
                case PUSH, LOAD -> {
                    if (sp == stack.length) stack = java.util.Arrays.copyOf(stack, sp * 2);
                    stack[sp++] = instr.op() == Op.PUSH ? instr.arg() : slots[(int) instr.arg()];
                }
                case STORE -> {
                    need(sp, 1);
                    slots[(int) instr.arg()] = stack[--sp];
                }
                case NEG -> {
                    need(sp, 1);
                    stack[sp - 1] = -stack[sp - 1];
                }
                case PRINT -> {
                    need(sp, 1);
                    output.add(stack[--sp]);
                }
                case JUMP -> pc = (int) instr.arg();
                case JUMP_IF_ZERO -> {
                    need(sp, 1);
                    if (stack[--sp] == 0) pc = (int) instr.arg();
                }
                default -> {
                    need(sp, 2);
                    long b = stack[--sp];
                    long a = stack[sp - 1];
                    stack[sp - 1] = switch (instr.op()) {
                        case ADD -> a + b;
                        case SUB -> a - b;
                        case MUL -> a * b;
                        case DIV, MOD -> {
                            if (b == 0) throw new VmError("division by zero at instruction " + (pc - 1));
                            yield instr.op() == Op.DIV ? a / b : a % b;
                        }
                        case LT -> a < b ? 1 : 0;
                        case LE -> a <= b ? 1 : 0;
                        case GT -> a > b ? 1 : 0;
                        case GE -> a >= b ? 1 : 0;
                        case EQ -> a == b ? 1 : 0;
                        case NE -> a != b ? 1 : 0;
                        default -> throw new IllegalStateException(instr.toString());
                    };
                }
            }
        }
        return output;
    }

    private static void need(int sp, int count) {
        if (sp < count) throw new VmError("stack underflow");
    }
}
