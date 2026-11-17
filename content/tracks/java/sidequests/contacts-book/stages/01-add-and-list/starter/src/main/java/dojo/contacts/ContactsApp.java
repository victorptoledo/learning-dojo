package dojo.contacts;

import java.io.PrintStream;

/** The contacts book from the terminal. See README.md. */
public final class ContactsApp {

    private ContactsApp() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /** Runs one command and returns its exit code. */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        err.println("todo: write the contacts book");
        return 1;
    }
}
