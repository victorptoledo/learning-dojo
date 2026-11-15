package dojo;

/** The mail library's interface. You cannot change it. */
public interface Mailer {
    void send(String address, String subject, String body);
}
