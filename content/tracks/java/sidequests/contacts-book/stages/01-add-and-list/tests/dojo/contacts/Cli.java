package dojo.contacts;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Runs the contacts book like a terminal would and keeps what it printed. */
final class Cli {

    record Result(int code, String out, String err) {
        List<String> lines() {
            return out.isBlank() ? List.of() : Arrays.asList(out.strip().split("\\R"));
        }
    }

    private Cli() {
    }

    static Result run(Path file, String... args) {
        List<String> all = new ArrayList<>(List.of("--file", file.toString()));
        all.addAll(List.of(args));
        return runRaw(all.toArray(String[]::new));
    }

    static Result runRaw(String... args) {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();
        int code;
        try (var outStream = new PrintStream(out, true, StandardCharsets.UTF_8);
             var errStream = new PrintStream(err, true, StandardCharsets.UTF_8)) {
            code = ContactsApp.run(args, outStream, errStream);
        }
        return new Result(code, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }
}
