package dojo;

/** The instructions of the stack virtual machine. */
public enum Op {
    /** Push {@code arg}. */
    PUSH,
    /** Push the value of variable slot {@code arg}. */
    LOAD,
    /** Pop a value into variable slot {@code arg}. */
    STORE,
    /** Pop b, pop a, push a op b. */
    ADD, SUB, MUL, DIV, MOD,
    /** Pop b, pop a, push 1 if the comparison holds, else 0. */
    LT, LE, GT, GE, EQ, NE,
    /** Pop a, push -a. */
    NEG,
    /** Pop a value and append it to the output. */
    PRINT,
    /** Continue at instruction {@code arg}. */
    JUMP,
    /** Pop a value; if it is 0, continue at instruction {@code arg}. */
    JUMP_IF_ZERO
}
