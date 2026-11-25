package dojo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SloController {
    private final SloTracker tracker;

    public SloController(SloTracker tracker) {
        this.tracker = tracker;
    }

    @GetMapping("/slo")
    public SloReport slo() {
        return tracker.report();
    }
}
