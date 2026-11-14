package dojo;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/** POST /signups and GET /members/{id}. */
@RestController
public class SignupController {
    private final MemberService members;

    public SignupController(MemberService members) {
        this.members = members;
    }

    @PostMapping("/signups")
    public ResponseEntity<Member> signup(@Valid @RequestBody SignupRequest request, UriComponentsBuilder uri) {
        Member member = members.register(request);
        return ResponseEntity.created(uri.path("/members/{id}").build(member.id())).body(member);
    }

    @GetMapping("/members/{id}")
    public Member member(@PathVariable long id) {
        return members.get(id);
    }
}
