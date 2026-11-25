package dojo;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/** Reads every timesheet in an inbox folder and writes the minutes per person and ISO week. */
public final class TimesheetImport {
    private static final CsvMapper<Entry> MAPPER = CsvMapper.of(Entry.class);
    private static final List<String> REQUIRED = List.of("person", "zone", "start", "end");

    private TimesheetImport() {}

    public static ImportResult run(Path inbox, Path report) throws IOException {
        List<Path> files;
        try (Stream<Path> entries = Files.list(inbox)) {
            files = entries.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".csv"))
                    .sorted()
                    .toList();
        }
        Map<String, TreeMap<String, Long>> minutes = new TreeMap<>();
        List<String> errors = new ArrayList<>();
        int count = 0;
        for (Path file : files) {
            count += read(file, minutes, errors);
        }
        StringBuilder out = new StringBuilder("person,week,minutes\n");
        minutes.forEach((person, weeks) -> weeks.forEach((week, total) ->
                out.append(person).append(',').append(week).append(',').append(total).append('\n')));
        write(report, out.toString());
        return new ImportResult(files.size(), count, errors);
    }

    private static int read(Path file, Map<String, TreeMap<String, Long>> minutes, List<String> errors)
            throws IOException {
        String name = file.getFileName().toString();
        int count = 0;
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                errors.add(name + ":1: the file is empty");
                return 0;
            }
            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }
            String[] header = headerLine.split(",", -1);
            for (int i = 0; i < header.length; i++) {
                header[i] = header[i].strip();
            }
            List<String> headerList = List.of(header);
            for (String column : REQUIRED) {
                if (!headerList.contains(column)) {
                    errors.add(name + ":1: missing column '" + column + "'");
                    return 0;
                }
            }
            String line;
            int number = 1;
            while ((line = reader.readLine()) != null) {
                number++;
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] fields = line.split(",", -1);
                if (fields.length != header.length) {
                    errors.add(name + ":" + number + ": expected " + header.length + " fields, got " + fields.length);
                    continue;
                }
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < header.length; i++) {
                    row.put(header[i], fields[i]);
                }
                try {
                    Entry entry = MAPPER.map(row);
                    long worked = Duration.between(entry.start().atZone(entry.zone()), entry.end().atZone(entry.zone()))
                            .toMinutes();
                    minutes.computeIfAbsent(entry.person(), p -> new TreeMap<>())
                            .merge(week(entry.start().toLocalDate()), worked, Long::sum);
                    count++;
                } catch (IllegalArgumentException e) {
                    errors.add(name + ":" + number + ": " + e.getMessage());
                }
            }
        }
        return count;
    }

    private static String week(LocalDate date) {
        return String.format("%d-W%02d", date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }

    private static void write(Path report, String content) throws IOException {
        Path folder = report.toAbsolutePath().getParent();
        Files.createDirectories(folder);
        Path temp = Files.createTempFile(folder, ".report", ".tmp");
        try {
            Files.writeString(temp, content);
            Files.move(temp, report, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
