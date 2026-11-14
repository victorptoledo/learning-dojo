package dojo;

import org.springframework.stereotype.Service;

@Service
public class MemberService {

    /** Registers a member; emails are stored stripped and lowercased, and must be unique. */
    public Member register(SignupRequest request) {
        throw new UnsupportedOperationException("todo register " + request);
    }

    /** The member, or MemberNotFoundException. */
    public Member get(long id) {
        throw new UnsupportedOperationException("todo get " + id);
    }
}
