package dojo;

import org.springframework.web.bind.annotation.RestController;

/** POST /signups and GET /members/{id}. */
@RestController
public class SignupController {
    private final MemberService members;

    public SignupController(MemberService members) {
        this.members = members;
    }
}
