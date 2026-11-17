package dojo.contacts;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** The contacts book from the terminal: one command per run, the book kept in a file. */
public final class ContactsApp {

    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private static final Pattern TAG = Pattern.compile("[a-z0-9-]+");
    private static final String USAGE =
            "usage: contacts --file <path> (add <name> <email> [<phone>] | list [--tag <tag>] | find <text>"
                    + " | remove <email> | tag <email> <tag> | export)";

    record Contact(String name, String email, String phone, TreeSet<String> tags) {
        String line() {
            var text = new StringBuilder(name).append(" <").append(email).append('>');
            if (!phone.isEmpty()) {
                text.append(' ').append(phone);
            }
            if (!tags.isEmpty()) {
                text.append(" [").append(String.join(", ", tags)).append(']');
            }
            return text.toString();
        }

        boolean hasEmail(String other) {
            return email.equalsIgnoreCase(other);
        }
    }

    /** A failed command: its exit code and message. */
    static final class Failure extends RuntimeException {
        final int code;

        Failure(int code, String message) {
            super(message);
            this.code = code;
        }
    }

    private ContactsApp() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    public static int run(String[] args, PrintStream out, PrintStream err) {
        try {
            if (args.length < 3 || !args[0].equals("--file")) {
                throw usage();
            }
            var book = new Book(Path.of(args[1]));
            return execute(book, Arrays.asList(args).subList(2, args.length), out);
        } catch (Failure failure) {
            err.println(failure.getMessage());
            return failure.code;
        } catch (UncheckedIOException e) {
            err.println("error: " + e.getCause().getMessage());
            return 1;
        }
    }

    private static int execute(Book book, List<String> command, PrintStream out) {
        String name = command.get(0);
        List<String> args = command.subList(1, command.size());
        switch (name) {
            case "add" -> {
                if (args.size() < 2 || args.size() > 3) {
                    throw usage();
                }
                String email = args.get(1);
                String phone = args.size() == 3 ? args.get(2) : "";
                book.contacts.add(new Contact(args.get(0), email, phone, new TreeSet<>()));
                book.save();
                out.println("added " + args.get(0));
                return 0;
            }
            case "list" -> {
                List<Contact> shown;
                if (args.isEmpty()) {
                    shown = book.sorted();
                } else {
                    throw usage();
                }
                if (shown.isEmpty()) {
                    out.println("no contacts");
                }
                shown.forEach(contact -> out.println(contact.line()));
                return 0;
            }
            default -> throw usage();
        }
    }

    private static Failure usage() {
        return new Failure(2, USAGE);
    }

    static String csv(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    /** The book in its file: one contact per line, tab-separated, tabs and backslashes escaped. */
    static final class Book {
        private final Path file;
        final List<Contact> contacts = new ArrayList<>();

        Book(Path file) {
            this.file = file;
            if (!Files.exists(file)) {
                return;
            }
            try {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    if (line.isEmpty()) {
                        continue;
                    }
                    String[] fields = line.split("\t", -1);
                    var tags = new TreeSet<String>();
                    if (!fields[3].isEmpty()) {
                        tags.addAll(List.of(fields[3].split(",")));
                    }
                    contacts.add(new Contact(unescape(fields[0]), unescape(fields[1]), unescape(fields[2]), tags));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        Optional<Contact> find(String email) {
            return contacts.stream().filter(contact -> contact.hasEmail(email)).findFirst();
        }

        Contact require(String email) {
            return find(email).orElseThrow(() -> new Failure(1, "no contact with email " + email));
        }

        List<Contact> sorted() {
            return contacts.stream()
                    .sorted(Comparator.comparing(Contact::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }

        void save() {
            String text = contacts.stream()
                    .map(contact -> String.join("\t", escape(contact.name()), escape(contact.email()),
                            escape(contact.phone()), String.join(",", contact.tags())))
                    .collect(Collectors.joining("\n", "", "\n"));
            try {
                Files.writeString(file, text, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private static String escape(String text) {
            return text.replace("\\", "\\\\").replace("\t", "\\t");
        }

        private static String unescape(String text) {
            var out = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\\' && i + 1 < text.length()) {
                    char next = text.charAt(++i);
                    out.append(next == 't' ? '\t' : next);
                } else {
                    out.append(c);
                }
            }
            return out.toString();
        }
    }
}
