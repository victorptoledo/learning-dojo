package dojo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Just enough JSON for JWT headers and claims: one flat object whose values are strings,
 * whole numbers or lists of strings. Keys keep their order. Throws IllegalArgumentException
 * on anything else.
 */
public final class MiniJson {
    private MiniJson() {}

    public static String write(Map<String, Object> object) {
        StringBuilder out = new StringBuilder("{");
        for (var entry : object.entrySet()) {
            if (out.length() > 1) {
                out.append(',');
            }
            out.append(quote(entry.getKey())).append(':');
            Object value = entry.getValue();
            if (value instanceof String text) {
                out.append(quote(text));
            } else if (value instanceof Long || value instanceof Integer) {
                out.append(value);
            } else if (value instanceof List<?> list) {
                out.append('[');
                for (int i = 0; i < list.size(); i++) {
                    out.append(i > 0 ? "," : "").append(quote((String) list.get(i)));
                }
                out.append(']');
            } else {
                throw new IllegalArgumentException("unsupported value " + value);
            }
        }
        return out.append('}').toString();
    }

    public static Map<String, Object> read(String json) {
        var parser = new Parser(json.strip());
        Map<String, Object> object = parser.object();
        if (parser.position != parser.text.length()) {
            throw new IllegalArgumentException("trailing text in " + json);
        }
        return object;
    }

    private static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static final class Parser {
        final String text;
        int position;

        Parser(String text) {
            this.text = text;
        }

        Map<String, Object> object() {
            Map<String, Object> object = new LinkedHashMap<>();
            expect('{');
            if (peek() == '}') {
                position++;
                return object;
            }
            do {
                String key = string();
                expect(':');
                object.put(key, value());
            } while (consume(','));
            expect('}');
            return object;
        }

        Object value() {
            char c = peek();
            if (c == '"') {
                return string();
            }
            if (c == '[') {
                position++;
                List<String> items = new ArrayList<>();
                if (peek() != ']') {
                    do {
                        items.add(string());
                    } while (consume(','));
                }
                expect(']');
                return items;
            }
            int start = position;
            if (c == '-') {
                position++;
            }
            while (position < text.length() && Character.isDigit(text.charAt(position))) {
                position++;
            }
            if (start == position) {
                throw new IllegalArgumentException("unexpected '" + c + "' at " + position);
            }
            return Long.parseLong(text.substring(start, position));
        }

        String string() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (true) {
                if (position >= text.length()) {
                    throw new IllegalArgumentException("unterminated string");
                }
                char c = text.charAt(position++);
                if (c == '"') {
                    return out.toString();
                }
                if (c == '\\') {
                    out.append(text.charAt(position++));
                } else {
                    out.append(c);
                }
            }
        }

        char peek() {
            skipSpaces();
            if (position >= text.length()) {
                throw new IllegalArgumentException("unexpected end of json");
            }
            return text.charAt(position);
        }

        boolean consume(char c) {
            if (peek() == c) {
                position++;
                return true;
            }
            return false;
        }

        void expect(char c) {
            if (!consume(c)) {
                throw new IllegalArgumentException("expected '" + c + "' at " + position);
            }
        }

        void skipSpaces() {
            while (position < text.length() && Character.isWhitespace(text.charAt(position))) {
                position++;
            }
        }
    }
}
