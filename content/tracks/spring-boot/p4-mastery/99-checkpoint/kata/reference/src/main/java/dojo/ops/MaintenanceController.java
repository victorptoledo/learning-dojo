package dojo.ops;

import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MaintenanceController {
    public record Maintenance(boolean on) {
    }

    private final ApplicationEventPublisher events;

    public MaintenanceController(ApplicationEventPublisher events) {
        this.events = events;
    }

    // Readiness tells the load balancer to stop sending traffic; liveness stays UP, so nobody restarts the pod.
    @PostMapping("/admin/maintenance")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void maintenance(@RequestBody Maintenance request) {
        ReadinessState state = request.on() ? ReadinessState.REFUSING_TRAFFIC : ReadinessState.ACCEPTING_TRAFFIC;
        AvailabilityChangeEvent.publish(events, this, state);
    }
}
