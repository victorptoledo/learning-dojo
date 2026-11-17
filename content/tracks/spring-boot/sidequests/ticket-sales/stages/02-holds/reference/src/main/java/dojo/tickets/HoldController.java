package dojo.tickets;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
public class HoldController {
    private final HoldService holds;

    public HoldController(HoldService holds) {
        this.holds = holds;
    }

    @PostMapping("/events/{eventId}/holds")
    public ResponseEntity<HoldView> hold(@PathVariable long eventId, @Valid @RequestBody HoldRequest request,
            Principal buyer, UriComponentsBuilder uri) {
        Hold hold = holds.hold(eventId, request, buyer.getName());
        return ResponseEntity.created(uri.path("/holds/{id}").build(hold.getId())).body(HoldView.of(hold, hold.getStatus()));
    }

    @GetMapping("/holds/{id}")
    public HoldView get(@PathVariable long id, Principal buyer) {
        Hold hold = holds.get(id, buyer.getName());
        return HoldView.of(hold, hold.getStatus());
    }

    @DeleteMapping("/holds/{id}")
    public ResponseEntity<Void> release(@PathVariable long id, Principal buyer) {
        holds.release(id, buyer.getName());
        return ResponseEntity.noContent().build();
    }
}
