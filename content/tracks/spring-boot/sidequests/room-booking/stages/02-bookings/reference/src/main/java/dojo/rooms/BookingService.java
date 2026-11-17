package dojo.rooms;

import java.time.LocalDate;
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
    private final Map<Long, Booking> bookings = new ConcurrentHashMap<>();
    private final AtomicLong lastId = new AtomicLong();

    public BookingService(RoomService rooms) {
        this.rooms = rooms;
    }

    public synchronized Booking book(long roomId, BookingRequest request) {
        if (!request.end().isAfter(request.start())) {
            throw new FieldException("end", "must be after start");
        }
        Room room = rooms.get(roomId);
        if (request.attendees() > room.capacity()) {
            throw new RuleException("room " + room.name() + " holds " + room.capacity() + " people");
        }
        bookings.values().stream()
                .filter(b -> b.roomId() == roomId && b.overlaps(request.start(), request.end()))
                .findFirst()
                .ifPresent(b -> {
                    throw new OverlapException(b);
                });
        Booking booking = new Booking(lastId.incrementAndGet(), roomId, request.title().strip(),
                request.start(), request.end(), request.attendees());
        bookings.put(booking.id(), booking);
        return booking;
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
