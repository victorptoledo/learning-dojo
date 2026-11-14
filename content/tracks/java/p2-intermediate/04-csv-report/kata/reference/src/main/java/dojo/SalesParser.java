package dojo;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Reads sales from comma-separated lines. */
public final class SalesParser {
    private SalesParser() {}

    public static List<Sale> parse(String text) {
        List<Sale> sales = new ArrayList<>();
        int number = 0;
        for (String line : text.lines().toList()) {
            number++;
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            sales.add(parseLine(number, trimmed));
        }
        return List.copyOf(sales);
    }

    private static Sale parseLine(int line, String text) {
        String[] fields = text.split(",", -1);
        if (fields.length != 5) {
            throw new BadLineException(line, "expected 5 fields but found " + fields.length);
        }
        for (int i = 0; i < fields.length; i++) {
            fields[i] = fields[i].strip();
        }
        LocalDate date;
        try {
            date = LocalDate.parse(fields[0]);
        } catch (DateTimeParseException e) {
            throw new BadLineException(line, "bad date '" + fields[0] + "'", e);
        }
        if (fields[1].isEmpty() || fields[2].isEmpty()) {
            throw new BadLineException(line, "missing region or product");
        }
        int units = number(line, fields[3], "units", 1);
        long price = number(line, fields[4], "price", 0);
        return new Sale(date, fields[1], fields[2], units, price);
    }

    private static int number(int line, String text, String name, int minimum) {
        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new BadLineException(line, "bad " + name + " '" + text + "'", e);
        }
        if (value < minimum) {
            throw new BadLineException(line, "bad " + name + " '" + text + "'");
        }
        return value;
    }
}
