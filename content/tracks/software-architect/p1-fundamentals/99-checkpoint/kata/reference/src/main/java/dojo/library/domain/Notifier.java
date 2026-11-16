package dojo.library.domain;

/** Driven port: telling a member something. */
public interface Notifier {
    void send(String memberId, String message);
}
