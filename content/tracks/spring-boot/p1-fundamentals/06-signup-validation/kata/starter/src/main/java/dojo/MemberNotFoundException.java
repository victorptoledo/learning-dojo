package dojo;

public class MemberNotFoundException extends RuntimeException {
    public MemberNotFoundException(long id) {
        super("member " + id + " not found");
    }
}
