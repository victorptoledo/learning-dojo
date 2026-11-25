package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TimesheetImportTest {
    @Test
    void maps_a_row_to_an_entry() {
        var mapper = CsvMapper.of(Entry.class);
        assertEquals(List.of("person", "zone", "start", "end", "note"), mapper.columns());
        Entry entry = mapper.map(Map.of("person", "Ada", "zone", "Europe/Lisbon",
                "start", "2026-11-23T09:00", "end", "2026-11-23T17:30"));
        assertEquals(new Entry("Ada", ZoneId.of("Europe/Lisbon"), LocalDateTime.parse("2026-11-23T09:00"),
                LocalDateTime.parse("2026-11-23T17:30"), null), entry);
    }

    @Test
    void writes_the_minutes_per_person_and_week(@TempDir Path dir) throws IOException {
        Path inbox = Files.createDirectory(dir.resolve("inbox"));
        Files.writeString(inbox.resolve("a.csv"), """
                person,zone,start,end,note
                Ada,Europe/Lisbon,2026-11-23T09:00,2026-11-23T17:30,
                """);
        Path report = dir.resolve("report.csv");
        assertEquals(new ImportResult(1, 1, List.of()), TimesheetImport.run(inbox, report));
        assertEquals("person,week,minutes\nAda,2026-W48,510\n", Files.readString(report));
    }
}
