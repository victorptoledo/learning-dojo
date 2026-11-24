package dojo.domain;

/** Driven port: tells a student something. */
public interface Notifier {
    void send(Student student, String message);
}
