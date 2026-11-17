package dojo.tickets;

import java.time.Clock;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HoldService {
    private final EventService events;
    private final SeatRepository seats;
    private final HoldRepository holds;
    private final TicketsProperties properties;
    private final Clock clock;

    public HoldService(EventService events, SeatRepository seats, HoldRepository holds, TicketsProperties properties,
            Clock clock) {
        this.events = events;
        this.seats = seats;
        this.holds = holds;
        this.properties = properties;
        this.clock = clock;
    }

    /** All or nothing: the seat rows are locked before anything is decided. */
    @Transactional
    public Hold hold(long eventId, HoldRequest request, String buyer) {
        List<String> wanted = request.seats();
        if (wanted.size() > properties.maxSeatsPerHold()) {
            throw new FieldException("seats", "at most " + properties.maxSeatsPerHold() + " seats per hold");
        }
        if (new HashSet<>(wanted).size() != wanted.size()) {
            throw new FieldException("seats", "a seat is asked for twice");
        }
        Event event = events.get(eventId);
        Map<String, Seat> locked = seats.lock(eventId, wanted).stream()
                .collect(Collectors.toMap(Seat::getLabel, Function.identity()));
        for (String label : wanted) {
            if (!locked.containsKey(label)) {
                throw new RuleException("unknown seat " + label);
            }
        }
        List<Seat> ordered = locked.values().stream()
                .sorted(Comparator.comparingInt(Seat::getRowIndex).thenComparingInt(Seat::getNumber))
                .toList();
        List<String> taken = ordered.stream().filter(s -> !s.isAvailable()).map(Seat::getLabel).toList();
        if (!taken.isEmpty()) {
            throw new UnavailableException(taken);
        }
        Hold hold = holds.save(new Hold(eventId, buyer, ordered.stream().map(Seat::getLabel).toList(),
                event.getPriceCents() * ordered.size(), clock.instant().plus(properties.holdTtl())));
        ordered.forEach(seat -> seat.hold(hold.getId()));
        return hold;
    }

    @Transactional(readOnly = true)
    public Hold get(long id, String buyer) {
        return holds.findById(id).filter(h -> h.getBuyer().equals(buyer))
                .orElseThrow(() -> new NotFoundException("hold " + id + " not found"));
    }

    @Transactional
    public void release(long id, String buyer) {
        Hold hold = holds.lock(id).filter(h -> h.getBuyer().equals(buyer))
                .orElseThrow(() -> new NotFoundException("hold " + id + " not found"));
        if (hold.getStatus() != HoldStatus.HELD) {
            throw new ConflictException("hold " + id + " is not active");
        }
        seats.lockHeldBy(id).forEach(Seat::release);
        hold.setStatus(HoldStatus.RELEASED);
    }
}
