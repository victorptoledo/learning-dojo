package dojo.notifications;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReminderController {
    private final Reminders reminders;

    public ReminderController(Reminders reminders) {
        this.reminders = reminders;
    }

    @GetMapping("/reminders")
    public List<Reminder> planned() {
        return reminders.planned();
    }
}
