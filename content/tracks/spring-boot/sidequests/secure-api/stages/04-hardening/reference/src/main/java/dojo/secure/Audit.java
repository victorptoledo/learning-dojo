package dojo.secure;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Audit {
    public record EventView(AuditEvent.Type type, String username, Instant at) {
    }

    private final AuditRepository events;
    private final Clock clock;

    public Audit(AuditRepository events, Clock clock) {
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public void record(AuditEvent.Type type, String username) {
        events.save(new AuditEvent(type, username, Instant.now(clock)));
    }

    @Transactional(readOnly = true)
    public List<EventView> latest(String username) {
        List<AuditEvent> found = username == null
                ? events.findTop100ByOrderByIdDesc()
                : events.findTop100ByUsernameOrderByIdDesc(username);
        return found.stream().map(e -> new EventView(e.getType(), e.getUsername(), e.getAt())).toList();
    }
}
