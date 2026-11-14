package dojo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class MemberService {
    private final Map<Long, Member> members = new ConcurrentHashMap<>();
    private final Map<String, Long> idsByEmail = new ConcurrentHashMap<>();
    private final AtomicLong lastId = new AtomicLong();

    /** Registers a member; emails are stored stripped and lowercased, and must be unique. */
    public synchronized Member register(SignupRequest request) {
        String email = request.email().strip().toLowerCase();
        if (idsByEmail.containsKey(email)) {
            throw new EmailTakenException(email);
        }
        Member member = new Member(lastId.incrementAndGet(), request.name().strip(), email, request.age());
        members.put(member.id(), member);
        idsByEmail.put(email, member.id());
        return member;
    }

    /** The member, or MemberNotFoundException. */
    public Member get(long id) {
        Member member = members.get(id);
        if (member == null) {
            throw new MemberNotFoundException(id);
        }
        return member;
    }
}
