package dojo.rooms;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/rooms")
public class RoomController {
    private final RoomService rooms;

    public RoomController(RoomService rooms) {
        this.rooms = rooms;
    }

    @PostMapping
    public ResponseEntity<Room> create(@Valid @RequestBody RoomRequest request, UriComponentsBuilder uri) {
        Room room = rooms.create(request);
        return ResponseEntity.created(uri.path("/rooms/{id}").build(room.id())).body(room);
    }

    @GetMapping
    public List<Room> all() {
        return rooms.all();
    }

    @GetMapping("/{id}")
    public Room get(@PathVariable long id) {
        return rooms.get(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        rooms.delete(id);
        return ResponseEntity.noContent().build();
    }
}
