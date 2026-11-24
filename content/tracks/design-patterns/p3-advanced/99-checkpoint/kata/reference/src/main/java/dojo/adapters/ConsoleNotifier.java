package dojo.adapters;

import dojo.domain.Notifier;
import dojo.domain.Student;

public final class ConsoleNotifier implements Notifier {
    @Override
    public void send(Student student, String message) {
        System.out.println("MAIL " + student.name() + ": " + message);
    }
}
