package dojo.notifications;

import java.time.LocalDate;

public record Reminder(String invoice, String customer, LocalDate remindOn) {
}
