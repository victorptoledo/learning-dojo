package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.adapters.InMemoryEnrolments;
import dojo.adapters.InMemoryStudents;
import dojo.domain.Course;
import dojo.domain.EnrolmentService;
import dojo.domain.Student;
import dojo.legacy.LegacyGradebook;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Describes the target design, so it does not compile until the ports, the adapters and the
 * composition root exist. Start with the domain types and the ports.
 */
class EnrolmentServiceTest {
    private final InMemoryStudents students = new InMemoryStudents();
    private final InMemoryEnrolments enrolments = new InMemoryEnrolments();
    private final LegacyGradebook gradebook = new LegacyGradebook();
    private final EnrolmentService service = App.enrolmentService(students, enrolments, gradebook);

    @Test
    void a_student_with_the_credits_and_the_prerequisite_is_enrolled() {
        students.add(new Student("s1", "Ana", 30));
        gradebook.put("s1", "MATH101:B+");
        assertEquals("enrolled", service.enrol("s1", new Course("MATH201", 30, 20, "MATH101")));
        assertEquals(List.of("MATH201"), enrolments.coursesOf("s1"));
    }

    @Test
    void a_failed_prerequisite_is_refused() {
        students.add(new Student("s2", "Bo", 30));
        gradebook.put("s2", "MATH101:F");
        assertEquals("refused: needs MATH101", service.enrol("s2", new Course("MATH201", 30, 20, "MATH101")));
    }
}
