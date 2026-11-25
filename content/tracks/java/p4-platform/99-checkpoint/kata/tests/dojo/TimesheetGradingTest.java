package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TimesheetGradingTest {
    private static final String HEADER = "person,zone,start,end,note\n";

    record Typed(@Column("name") String name, @Column("count") int count, @Column("big") long big,
            @Column("day") LocalDate day, @Column("at") LocalDateTime at, @Column("zone") ZoneId zone,
            @Column(value = "comment", required = false) String comment) {}

    record NoColumn(@Column("a") String a, String b) {}

    record Unsupported(@Column("price") double price) {}

    record OptionalPrimitive(@Column(value = "count", required = false) int count) {}

    record Twice(@Column("x") String a, @Column("x") String b) {}

    private static Map<String, String> typedRow() {
        return Map.of("name", " Ada ", "count", "3", "big", "9000000000", "day", "2026-11-25",
                "at", "2026-11-25T09:15", "zone", "America/Sao_Paulo", "extra", "ignored");
    }

    // ---- the mapper ----------------------------------------------------------------------

    @Test
    void the_mapper_reads_every_supported_type_in_component_order() {
        var mapper = CsvMapper.of(Typed.class);
        assertEquals(List.of("name", "count", "big", "day", "at", "zone", "comment"), mapper.columns());
        assertEquals(new Typed("Ada", 3, 9_000_000_000L, LocalDate.of(2026, 11, 25),
                LocalDateTime.of(2026, 11, 25, 9, 15), ZoneId.of("America/Sao_Paulo"), null), mapper.map(typedRow()));
    }

    @Test
    void a_missing_required_value_is_an_error_and_an_optional_one_is_null() {
        var mapper = CsvMapper.of(Typed.class);
        var row = new java.util.HashMap<>(typedRow());
        row.put("comment", "   ");
        assertNull(mapper.map(row).comment());
        row.put("count", " ");
        var e = assertThrows(IllegalArgumentException.class, () -> mapper.map(row));
        assertEquals("missing column 'count'", e.getMessage());
        row.remove("count");
        assertEquals("missing column 'count'", assertThrows(IllegalArgumentException.class, () -> mapper.map(row)).getMessage());
    }

    @Test
    void a_bad_value_names_the_column_the_text_and_the_type() {
        var mapper = CsvMapper.of(Typed.class);
        var row = new java.util.HashMap<>(typedRow());
        row.put("at", "25/11/2026 09:15");
        var e = assertThrows(IllegalArgumentException.class, () -> mapper.map(row));
        assertEquals("column 'at': cannot read '25/11/2026 09:15' as LocalDateTime", e.getMessage());
        row.put("at", "2026-11-25T09:15");
        row.put("zone", "Mars/Olympus");
        var zone = assertThrows(IllegalArgumentException.class, () -> mapper.map(row));
        assertEquals("column 'zone': cannot read 'Mars/Olympus' as ZoneId", zone.getMessage());
    }

    @Test
    void the_record_constructor_error_comes_out_unwrapped() {
        var e = assertThrows(IllegalArgumentException.class, () -> CsvMapper.of(Entry.class).map(Map.of(
                "person", "Ada", "zone", "Europe/Lisbon", "start", "2026-11-23T17:00", "end", "2026-11-23T09:00")));
        assertEquals("end must be after start", e.getMessage());
    }

    @Test
    void the_mapper_checks_the_record_when_it_is_built() {
        var none = assertThrows(IllegalArgumentException.class, () -> CsvMapper.of(NoColumn.class));
        assertTrue(none.getMessage().contains("NoColumn.b"), none.getMessage());
        var type = assertThrows(IllegalArgumentException.class, () -> CsvMapper.of(Unsupported.class));
        assertTrue(type.getMessage().contains("Unsupported.price"), type.getMessage());
        var optional = assertThrows(IllegalArgumentException.class, () -> CsvMapper.of(OptionalPrimitive.class));
        assertTrue(optional.getMessage().contains("OptionalPrimitive.count"), optional.getMessage());
        var twice = assertThrows(IllegalArgumentException.class, () -> CsvMapper.of(Twice.class));
        assertTrue(twice.getMessage().contains("Twice.b"), twice.getMessage());
    }

    // ---- the import ----------------------------------------------------------------------

    private static Path inbox(Path dir, String... namesAndContents) throws IOException {
        Path inbox = Files.createDirectories(dir.resolve("inbox"));
        for (int i = 0; i < namesAndContents.length; i += 2) {
            Files.writeString(inbox.resolve(namesAndContents[i]), namesAndContents[i + 1]);
        }
        return inbox;
    }

    @Test
    void adds_up_minutes_per_person_and_iso_week_in_order(@TempDir Path dir) throws IOException {
        Path inbox = inbox(dir,
                "b-week.csv", HEADER
                        + "Linus,Europe/Lisbon,2026-11-24T09:00,2026-11-24T12:00,\n"
                        + "Ada,Europe/Lisbon,2026-11-30T09:00,2026-11-30T10:00,new week\n",
                "a-week.csv", HEADER
                        + "Ada,Europe/Lisbon,2026-11-23T09:00,2026-11-23T17:30,\n"
                        + "Ada,America/Sao_Paulo,2026-11-25T08:00,2026-11-25T08:45,remote\n");
        Path report = dir.resolve("out").resolve("report.csv");
        assertEquals(new ImportResult(2, 4, List.of()), TimesheetImport.run(inbox, report));
        assertEquals("""
                person,week,minutes
                Ada,2026-W48,555
                Ada,2026-W49,60
                Linus,2026-W48,180
                """, Files.readString(report));
    }

    @Test
    void counts_real_time_across_daylight_saving_changes(@TempDir Path dir) throws IOException {
        Path inbox = inbox(dir, "dst.csv", HEADER
                + "Ada,Europe/Lisbon,2026-03-29T00:30,2026-03-29T03:30,clocks forward\n"
                + "Linus,Europe/Lisbon,2026-10-25T00:30,2026-10-25T03:30,clocks back\n");
        Path report = dir.resolve("report.csv");
        TimesheetImport.run(inbox, report);
        assertEquals("person,week,minutes\nAda,2026-W13,120\nLinus,2026-W43,240\n", Files.readString(report));
    }

    @Test
    void weeks_follow_the_week_based_year(@TempDir Path dir) throws IOException {
        Path inbox = inbox(dir, "new-year.csv", HEADER
                + "Ada,UTC,2026-12-28T09:00,2026-12-28T10:00,\n"
                + "Ada,UTC,2027-01-01T09:00,2027-01-01T10:00,\n"
                + "Ada,UTC,2027-01-04T09:00,2027-01-04T10:00,\n"
                + "Ada,UTC,2027-12-31T09:00,2027-12-31T10:00,\n");
        Path report = dir.resolve("report.csv");
        TimesheetImport.run(inbox, report);
        assertEquals("person,week,minutes\nAda,2026-W53,120\nAda,2027-W01,60\nAda,2027-W52,60\n", Files.readString(report));
    }

    @Test
    void bad_lines_become_errors_with_file_and_line_and_the_rest_still_counts(@TempDir Path dir) throws IOException {
        Path inbox = inbox(dir,
                "a.csv", HEADER
                        + "# exported by the clock app\n"
                        + "\n"
                        + "Ada,Europe/Lisbon,2026-11-23T09:00,2026-11-23T10:00,\n"
                        + "Ada,Europe/Lisbon,2026-11-23T09:00\n"
                        + "Ada,Europe/Lisboa,2026-11-23T09:00,2026-11-23T10:00,\n"
                        + "Ada,Europe/Lisbon,2026-11-23T11:00,2026-11-23T10:00,\n"
                        + ",Europe/Lisbon,2026-11-23T09:00,2026-11-23T10:00,\n",
                "b.csv", "person,zone,start,note\nAda,UTC,2026-11-23T09:00,\n");
        Path report = dir.resolve("report.csv");
        ImportResult result = TimesheetImport.run(inbox, report);
        assertEquals(new ImportResult(2, 1, List.of(
                "a.csv:5: expected 5 fields, got 3",
                "a.csv:6: column 'zone': cannot read 'Europe/Lisboa' as ZoneId",
                "a.csv:7: end must be after start",
                "a.csv:8: missing column 'person'",
                "b.csv:1: missing column 'end'")), result);
        assertEquals("person,week,minutes\nAda,2026-W48,60\n", Files.readString(report));
    }

    @Test
    void reads_utf8_with_or_without_a_byte_order_mark_and_columns_in_any_order(@TempDir Path dir) throws IOException {
        Path inbox = Files.createDirectories(dir.resolve("inbox"));
        Files.write(inbox.resolve("bom.csv"), ("\uFEFFzone,end,person,start,note\n"
                + "Europe/Lisbon,2026-11-23T10:00,João,2026-11-23T09:00,café\n").getBytes(StandardCharsets.UTF_8));
        Path report = dir.resolve("report.csv");
        assertEquals(new ImportResult(1, 1, List.of()), TimesheetImport.run(inbox, report));
        assertEquals("person,week,minutes\nJoão,2026-W48,60\n", Files.readString(report, StandardCharsets.UTF_8));
    }

    @Test
    void only_csv_files_directly_in_the_inbox_are_read(@TempDir Path dir) throws IOException {
        Path inbox = inbox(dir,
                "a.csv", HEADER + "Ada,UTC,2026-11-23T09:00,2026-11-23T10:00,\n",
                "notes.txt", HEADER + "Ada,UTC,2026-11-23T09:00,2026-11-23T19:00,\n");
        Path folder = Files.createDirectory(inbox.resolve("old.csv"));
        Files.writeString(folder.resolve("x.csv"), HEADER + "Ada,UTC,2026-11-23T09:00,2026-11-23T19:00,\n");
        Path report = dir.resolve("report.csv");
        assertEquals(new ImportResult(1, 1, List.of()), TimesheetImport.run(inbox, report));
        assertEquals("person,week,minutes\nAda,2026-W48,60\n", Files.readString(report));
    }

    @Test
    void the_report_is_replaced_in_one_step_and_no_temp_file_is_left(@TempDir Path dir) throws IOException {
        Path inbox = inbox(dir, "a.csv", HEADER + "Ada,UTC,2026-11-23T09:00,2026-11-23T10:00,\n");
        Path out = Files.createDirectories(dir.resolve("out"));
        Path report = out.resolve("report.csv");
        Files.writeString(report, "old report\n");
        TimesheetImport.run(inbox, report);
        assertEquals("person,week,minutes\nAda,2026-W48,60\n", Files.readString(report));
        try (Stream<Path> left = Files.list(out)) {
            assertEquals(List.of(report), left.toList(), "only the report may stay in its folder");
        }
        String code = Files.readString(Path.of("src", "main", "java", "dojo", "TimesheetImport.java"));
        assertTrue(code.contains("ATOMIC_MOVE"), "write to a temp file next to the report, then move it with ATOMIC_MOVE");
    }

    @Test
    void an_empty_inbox_gives_an_empty_report_and_a_missing_one_leaves_the_old_report(@TempDir Path dir)
            throws IOException {
        Path inbox = Files.createDirectory(dir.resolve("inbox"));
        Path report = dir.resolve("report.csv");
        assertEquals(new ImportResult(0, 0, List.of()), TimesheetImport.run(inbox, report));
        assertEquals("person,week,minutes\n", Files.readString(report));
        Files.writeString(report, "yesterday\n");
        assertThrows(NoSuchFileException.class, () -> TimesheetImport.run(dir.resolve("nowhere"), report));
        assertEquals("yesterday\n", Files.readString(report));
    }
}
