package dojo;

import java.time.LocalDate;

public record EntryView(long id, LocalDate bookedOn, String description, long amount) {}
