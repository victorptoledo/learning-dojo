package dojo;

/** One instruction; {@code arg} is 0 for instructions that take no argument. */
public record Instr(Op op, long arg) {
    public static Instr of(Op op) {
        return new Instr(op, 0);
    }

    public static Instr of(Op op, long arg) {
        return new Instr(op, arg);
    }

    @Override
    public String toString() {
        return switch (op) {
            case PUSH, LOAD, STORE, JUMP, JUMP_IF_ZERO -> op + " " + arg;
            default -> op.toString();
        };
    }
}
