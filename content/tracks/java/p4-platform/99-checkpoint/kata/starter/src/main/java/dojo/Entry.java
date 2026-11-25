package dojo;

import java.time.LocalDateTime;
import java.time.ZoneId;

/** One shift as a person typed it: wall-clock times in the zone where they worked. Already written. */
public record Entry(
        @Column("person") String person,
        @Column("zone") ZoneId zone,
        @Column("start") LocalDateTime start,
        @Column("end") LocalDateTime end,
        @Column(value = "note", required = false) String note) {

    public Entry {
        if (person == null || person.isBlank()) {
            throw new IllegalArgumentException("person is required");
        }
        if (!end.atZone(zone).isAfter(start.atZone(zone))) {
            throw new IllegalArgumentException("end must be after start");
        }
    }
}
