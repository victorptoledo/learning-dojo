package dojo.rooms;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/** Keeps the bookings in memory; one lock makes "check, then add" atomic. */
@Service
public class BookingService {
    private final RoomService rooms;
    private final BookingRules rules;
    private final Clock clock;
    private final Map<Long, Booking> bookings = new ConcurrentHashMap<>();
    private final AtomicLong lastId = new AtomicLong();

    public BookingService(RoomService rooms, BookingRules rules, Clock clock) {
        this.rooms = rooms;
        this.rules = rules;
        this.clock = clock;
    }

    public synchronized Booking book(long roomId, BookingRequest request) {
        LocalDateTime start = request.start();
        LocalDateTime end = request.end();
        if (!end.isAfter(start)) {
            throw new FieldException("end", "must be after start");
        }
        Room room = rooms.get(roomId);
        checkRules(start, end);
        if (request.attendees() > room.capacity()) {
            throw new RuleException("room " + room.name() + " holds " + room.capacity() + " people");
        }
        bookings.values().stream()
                .filter(b -> b.roomId() == roomId && b.overlaps(start, end))
                .findFirst()
                .ifPresent(b -> {
                    throw new OverlapException(b);
                });
        Booking booking = new Booking(lastId.incrementAndGet(), roomId, request.title().strip(), start, end,
                request.attendees());
        bookings.put(booking.id(), booking);
        return booking;
    }

    private void checkRules(LocalDateTime start, LocalDateTime end) {
        if (start.isBefore(LocalDateTime.now(clock))) {
            throw new RuleException("a booking cannot start in the past");
        }
        boolean sameDay = start.toLocalDate().equals(end.toLocalDate());
        if (!sameDay || start.toLocalTime().isBefore(rules.opens()) || end.toLocalTime().isAfter(rules.closes())) {
            throw new RuleException("the building is open from " + rules.opens() + " to " + rules.closes());
        }
        if (Duration.between(start, end).compareTo(rules.maxDuration()) > 0) {
            throw new RuleException("a booking lasts at most " + rules.maxDuration().toMinutes() + " minutes");
        }
    }

    public Booking get(long id) {
        Booking booking = bookings.get(id);
        if (booking == null) {
            throw new NotFoundException("booking", id);
        }
        return booking;
    }

    /** The bookings of a room by start, only those starting on `date` when it is given. */
    public List<Booking> ofRoom(long roomId, LocalDate date) {
        rooms.get(roomId);
        return bookings.values().stream()
                .filter(b -> b.roomId() == roomId)
                .filter(b -> date == null || b.start().toLocalDate().equals(date))
                .sorted(Comparator.comparing(Booking::start))
                .toList();
    }

    /** The stretches of the opening hours of `date` that no booking of the room covers. */
    public List<Slot> freeSlots(long roomId, LocalDate date) {
        List<Slot> free = new ArrayList<>();
        LocalDateTime cursor = date.atTime(rules.opens());
        LocalDateTime closes = date.atTime(rules.closes());
        for (Booking booking : ofRoom(roomId, date)) {
            if (booking.start().isAfter(cursor)) {
                free.add(new Slot(cursor, booking.start()));
            }
            if (booking.end().isAfter(cursor)) {
                cursor = booking.end();
            }
        }
        if (cursor.isBefore(closes)) {
            free.add(new Slot(cursor, closes));
        }
        return free;
    }

    public synchronized void cancel(long id) {
        if (bookings.remove(id) == null) {
            throw new NotFoundException("booking", id);
        }
    }

    /** Deletes a room that has no bookings left. */
    public synchronized void deleteRoom(long roomId) {
        rooms.get(roomId);
        if (bookings.values().stream().anyMatch(b -> b.roomId() == roomId)) {
            throw new ConflictException("room " + roomId + " still has bookings");
        }
        rooms.delete(roomId);
    }
}
