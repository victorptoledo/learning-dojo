package dojo;

import java.util.List;

/** Runs stack-machine bytecode and returns what it printed. */
public final class Vm {
    public static final long DEFAULT_MAX_STEPS = 100_000_000;

    private Vm() {}

    public static List<Long> run(List<Instr> code) {
        return run(code, DEFAULT_MAX_STEPS);
    }

    public static List<Long> run(List<Instr> code, long maxSteps) {
        throw new UnsupportedOperationException("todo: run " + code.size() + " instructions");
    }
}
