package dojo;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Structure checks for the grading tests: reflection plus a light reading of the sources. */
final class GradingSupport {
    private static final Path SOURCES = Path.of("src", "main", "java");

    private GradingSupport() {}

    /** The class with this fully qualified name, or a failed test that names it. */
    static Class<?> type(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return fail("expected a type named " + name);
        }
    }

    /** The source of a class, with comments, string literals and char literals blanked. */
    static String code(String qualifiedName) {
        return strip(rawSource(qualifiedName));
    }

    /** The source of a class as written. */
    static String rawSource(String qualifiedName) {
        Path file = SOURCES.resolve(qualifiedName.replace('.', '/') + ".java");
        try {
            return Files.readString(file);
        } catch (IOException e) {
            return fail("cannot read " + file + ": keep each public class in its own file");
        }
    }

    /** Every Java file under src/main/java, as qualified class names. */
    static List<String> allClasses() {
        try (var files = Files.walk(SOURCES)) {
            return files.filter(path -> path.toString().endsWith(".java"))
                    .map(path -> SOURCES.relativize(path).toString()
                            .replace('\\', '/')
                            .replace('/', '.')
                            .replaceAll("\\.java$", ""))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return fail("cannot list the sources: " + e.getMessage());
        }
    }

    /** The types of the instance fields a class declares. */
    static List<Class<?>> instanceFieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .filter(field -> !field.isSynthetic())
                .<Class<?>>map(Field::getType)
                .toList();
    }

    /** Number of times a regular expression matches. */
    static int count(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        int found = 0;
        while (matcher.find()) {
            found++;
        }
        return found;
    }

    /** Line counts of the method and constructor bodies in a source (comments stripped). */
    static List<Integer> bodyLengths(String code) {
        List<Integer> lengths = new ArrayList<>();
        // A body opens after `) {` or `) throws X {`: method and constructor declarations.
        Matcher opener = Pattern.compile("\\)\\s*(throws\\s+[\\w.,\\s]+)?\\{").matcher(code);
        int from = 0;
        while (opener.find(from)) {
            String before = code.substring(Math.max(0, opener.start() - 200), opener.start());
            int open = opener.end() - 1;
            int close = matching(code, open);
            boolean controlFlow = before.matches("(?s).*\\b(if|for|while|switch|catch|synchronized|try)\\s*\\([^;{}]*$");
            boolean recordHeader = before.matches("(?s).*\\brecord\\s+\\w+\\s*(<[^>]*>)?\\s*\\([^;{}]*$");
            if (!controlFlow && !recordHeader) {
                String body = code.substring(open + 1, close);
                lengths.add((int) body.lines().filter(line -> !line.isBlank()).count());
                from = close;
            } else {
                from = opener.end();
            }
        }
        return lengths;
    }

    private static int matching(String code, int open) {
        int depth = 0;
        for (int i = open; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c == '{') depth++;
            if (c == '}' && --depth == 0) return i;
        }
        return code.length() - 1;
    }

    private static String strip(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
            if (c == '/' && next == '/') {
                while (i < source.length() && source.charAt(i) != '\n') i++;
            } else if (c == '/' && next == '*') {
                int end = source.indexOf("*/", i + 2);
                String comment = source.substring(i, end < 0 ? source.length() : end + 2);
                out.append(comment.replaceAll("[^\\n]", " "));
                i = end < 0 ? source.length() : end + 2;
            } else if (source.startsWith("\"\"\"", i)) {
                int end = source.indexOf("\"\"\"", i + 3);
                String block = source.substring(i, end < 0 ? source.length() : end + 3);
                out.append("\"\"").append(block.replaceAll("[^\\n]", "").replace("\n", "\n"));
                i = end < 0 ? source.length() : end + 3;
            } else if (c == '"' || c == '\'') {
                int j = i + 1;
                while (j < source.length() && source.charAt(j) != c) {
                    if (source.charAt(j) == '\\') j++;
                    j++;
                }
                out.append(c).append(c);
                i = j + 1;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }
}
