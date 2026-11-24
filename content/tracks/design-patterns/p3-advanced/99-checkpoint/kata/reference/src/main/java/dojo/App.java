package dojo;

import dojo.adapters.ConsoleNotifier;
import dojo.adapters.GradebookTranscripts;
import dojo.adapters.InMemoryEnrolments;
import dojo.adapters.InMemoryStudents;
import dojo.domain.EnrolmentService;
import dojo.legacy.LegacyGradebook;

/** The composition root: the only place that knows both the domain and the adapters. */
public final class App {
    private App() {}

    public static EnrolmentService enrolmentService(
            InMemoryStudents students, InMemoryEnrolments enrolments, LegacyGradebook gradebook) {
        return new EnrolmentService(students, enrolments, new GradebookTranscripts(gradebook), new ConsoleNotifier());
    }
}
