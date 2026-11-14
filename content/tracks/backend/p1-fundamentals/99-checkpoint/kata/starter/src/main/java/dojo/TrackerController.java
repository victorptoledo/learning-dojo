package dojo;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class TrackerController {
    private final TrackerService tracker;

    public TrackerController(TrackerService tracker) {
        this.tracker = tracker;
    }
}
