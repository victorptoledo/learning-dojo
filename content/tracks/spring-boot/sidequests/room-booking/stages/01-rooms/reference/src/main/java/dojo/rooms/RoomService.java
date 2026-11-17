package dojo.rooms;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/** Keeps the rooms in memory. */
@Service
public class RoomService {
    private final Map<Long, Room> rooms = new ConcurrentSkipListMap<>();
    private final AtomicLong lastId = new AtomicLong();

    public synchronized Room create(RoomRequest request) {
        String name = request.name().strip();
        String key = name.toLowerCase(Locale.ROOT);
        boolean taken = rooms.values().stream().anyMatch(r -> r.name().toLowerCase(Locale.ROOT).equals(key));
        if (taken) {
            throw new ConflictException("room " + name + " already exists");
        }
        Room room = new Room(lastId.incrementAndGet(), name, request.capacity());
        rooms.put(room.id(), room);
        return room;
    }

    public List<Room> all() {
        return List.copyOf(rooms.values());
    }

    public Room get(long id) {
        Room room = rooms.get(id);
        if (room == null) {
            throw new NotFoundException("room", id);
        }
        return room;
    }

    public synchronized void delete(long id) {
        if (rooms.remove(id) == null) {
            throw new NotFoundException("room", id);
        }
    }
}
