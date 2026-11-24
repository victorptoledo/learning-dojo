package dojo;

import static dojo.GradingSupport.allClasses;
import static dojo.GradingSupport.code;
import static dojo.GradingSupport.instanceFieldTypes;
import static dojo.GradingSupport.type;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.adapters.GradebookTranscripts;
import dojo.adapters.InMemoryEnrolments;
import dojo.adapters.InMemoryStudents;
import dojo.domain.Applicant;
import dojo.domain.Course;
import dojo.domain.EnrolmentRules;
import dojo.domain.EnrolmentService;
import dojo.domain.Enrolments;
import dojo.domain.Grade;
import dojo.domain.Notifier;
import dojo.domain.Requirement;
import dojo.domain.Specification;
import dojo.domain.Student;
import dojo.domain.Students;
import dojo.domain.Transcripts;
import dojo.legacy.LegacyGradebook;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EnrolmentGradingTest {
    private final InMemoryStudents students = new InMemoryStudents();
    private final InMemoryEnrolments enrolments = new InMemoryEnrolments();
    private final Map<String, List<Grade>> grades = new HashMap<>();
    private final List<String> mails = new ArrayList<>();
    private final EnrolmentService service = new EnrolmentService(
            students,
            enrolments,
            id -> grades.getOrDefault(id, List.of()),
            (student, message) -> mails.add(student.name() + ": " + message));

    private static final Course ALGEBRA = new Course("MATH201", 2, 20, "MATH101");
    private static final Course ART = new Course("ART100", 30, 0, null);

    private Student student(String id, int credits) {
        Student student = new Student(id, "name-" + id, credits);
        students.add(student);
        return student;
    }

    private static Applicant applicant(int credits, List<Grade> transcript, List<String> courses) {
        return new Applicant(new Student("x", "X", credits), transcript, courses);
    }

    // ---- the use case ---------------------------------------------------------------

    @Test
    void a_student_who_meets_every_rule_is_enrolled_and_told() {
        student("s1", 30);
        grades.put("s1", List.of(new Grade("MATH101", 3.3)));
        assertEquals("enrolled", service.enrol("s1", ALGEBRA));
        assertEquals(List.of("MATH201"), enrolments.coursesOf("s1"));
        assertEquals(List.of("name-s1: enrolled in MATH201"), mails);
    }

    @Test
    void an_unknown_student_is_an_error() {
        var error = assertThrows(IllegalArgumentException.class, () -> service.enrol("ghost", ART));
        assertEquals("unknown student: ghost", error.getMessage());
    }

    @Test
    void refusals_come_in_the_order_of_the_rules() {
        student("low", 10);
        assertEquals("refused: needs 20 credits", service.enrol("low", ALGEBRA), "credits are checked first");
        student("nopre", 20);
        assertEquals("refused: needs MATH101", service.enrol("nopre", ALGEBRA), "exactly the minimum is enough credits");
        student("failed", 40);
        grades.put("failed", List.of(new Grade("MATH101", 0.0)));
        assertEquals("refused: needs MATH101", service.enrol("failed", ALGEBRA), "an F does not pass the prerequisite");
        student("busy", 40);
        for (String course : List.of("A1", "A2", "A3", "A4", "A5")) {
            enrolments.add("busy", course);
        }
        assertEquals("refused: too many courses", service.enrol("busy", ART));
        student("twice", 40);
        enrolments.add("twice", "ART100");
        assertEquals("refused: already enrolled", service.enrol("twice", ART));
    }

    @Test
    void a_full_course_is_refused_last() {
        for (String id : List.of("a", "b", "c")) {
            student(id, 40);
            grades.put(id, List.of(new Grade("MATH101", 0.7)));
        }
        assertEquals("enrolled", service.enrol("a", ALGEBRA), "a D- passes");
        assertEquals("enrolled", service.enrol("b", ALGEBRA));
        assertEquals("refused: course full", service.enrol("c", ALGEBRA));
    }

    @Test
    void a_refusal_records_nothing_and_tells_nobody() {
        student("low", 0);
        service.enrol("low", ALGEBRA);
        assertEquals(List.of(), enrolments.coursesOf("low"));
        assertEquals(0, enrolments.countIn("MATH201"));
        assertEquals(List.of(), mails);
    }

    @Test
    void courses_validate_themselves() {
        assertEquals("course code is required",
                assertThrows(IllegalArgumentException.class, () -> new Course(" ", 1, 0, null)).getMessage());
        assertThrows(IllegalArgumentException.class, () -> new Course("X", 0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new Course("X", 1, -1, null));
    }

    // ---- specifications -------------------------------------------------------------

    @Test
    void specifications_combine_with_and_or_and_not() {
        Specification<Integer> even = n -> n % 2 == 0;
        Specification<Integer> positive = n -> n > 0;
        assertTrue(even.and(positive).isSatisfiedBy(4));
        assertFalse(even.and(positive).isSatisfiedBy(-4));
        assertTrue(even.or(positive).isSatisfiedBy(3));
        assertFalse(even.or(positive).isSatisfiedBy(-3));
        assertTrue(even.not().isSatisfiedBy(3));
        assertFalse(even.not().isSatisfiedBy(2));
        assertTrue(even.not().and(positive.not()).isSatisfiedBy(-3));
    }

    @Test
    void each_rule_is_a_specification_on_its_own() {
        assertTrue(EnrolmentRules.hasCredits(20).isSatisfiedBy(applicant(20, List.of(), List.of())));
        assertFalse(EnrolmentRules.hasCredits(20).isSatisfiedBy(applicant(19, List.of(), List.of())));

        var transcript = List.of(new Grade("MATH101", 0.7), new Grade("PHYS101", 0.0));
        assertTrue(EnrolmentRules.passed("MATH101").isSatisfiedBy(applicant(0, transcript, List.of())));
        assertFalse(EnrolmentRules.passed("PHYS101").isSatisfiedBy(applicant(0, transcript, List.of())));
        assertFalse(EnrolmentRules.passed("CHEM101").isSatisfiedBy(applicant(0, transcript, List.of())));

        assertTrue(EnrolmentRules.takingFewerThan(2).isSatisfiedBy(applicant(0, List.of(), List.of("A"))));
        assertFalse(EnrolmentRules.takingFewerThan(2).isSatisfiedBy(applicant(0, List.of(), List.of("A", "B"))));
        assertTrue(EnrolmentRules.enrolledIn("A").isSatisfiedBy(applicant(0, List.of(), List.of("A"))));
        assertFalse(EnrolmentRules.enrolledIn("B").isSatisfiedBy(applicant(0, List.of(), List.of("A"))));
    }

    @Test
    void a_course_lists_its_requirements_in_order() {
        assertEquals(List.of("needs 20 credits", "needs MATH101", "too many courses", "already enrolled"),
                EnrolmentRules.forCourse(ALGEBRA).stream().map(Requirement::reason).toList());
        assertEquals(List.of("needs 0 credits", "too many courses", "already enrolled"),
                EnrolmentRules.forCourse(ART).stream().map(Requirement::reason).toList(),
                "no prerequisite, no prerequisite rule");
        assertEquals(5, EnrolmentRules.MAX_COURSES);
    }

    // ---- anti-corruption layer ------------------------------------------------------

    @Test
    void the_gradebook_is_translated_into_grade_points() {
        var gradebook = new LegacyGradebook();
        gradebook.put("s1", "MATH101:B+;PHYS201:C;;broken;CHEM:Q;BIO:A+;ART:F-;HIST:D-;GEO:B-; LAT : A");
        List<Grade> grades = new GradebookTranscripts(gradebook).of("s1");
        assertEquals(List.of("MATH101", "PHYS201", "BIO", "ART", "HIST", "GEO", "LAT"),
                grades.stream().map(Grade::course).toList(), "broken entries and unknown letters are skipped");
        double[] expected = {3.3, 2.0, 4.0, 0.0, 0.7, 2.7, 4.0};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], grades.get(i).points(), 1e-9, grades.get(i).course());
        }
        assertEquals(List.of(), new GradebookTranscripts(gradebook).of("nobody"));
    }

    @Test
    void the_composition_root_wires_the_real_adapters() {
        var gradebook = new LegacyGradebook();
        var root = App.enrolmentService(students, enrolments, gradebook);
        students.add(new Student("s9", "Rui", 25));
        gradebook.put("s9", "MATH101:D");
        assertEquals("enrolled", root.enrol("s9", ALGEBRA));
        gradebook.put("s9", "MATH101:F");
        students.add(new Student("s8", "Eva", 25));
        gradebook.put("s8", "MATH101:F");
        assertEquals("refused: needs MATH101", root.enrol("s8", ALGEBRA));
    }

    // ---- structure ------------------------------------------------------------------

    @Test
    void the_ports_are_interfaces_and_the_service_holds_only_them() {
        for (Class<?> port : List.of(Students.class, Enrolments.class, Transcripts.class, Notifier.class)) {
            assertTrue(port.isInterface(), port.getSimpleName() + " should be an interface");
        }
        assertTrue(Specification.class.isInterface());
        List<Class<?>> fields = instanceFieldTypes(EnrolmentService.class);
        assertEquals(4, fields.size(), "EnrolmentService holds its four ports and nothing else");
        assertEquals(Set.of(Students.class, Enrolments.class, Transcripts.class, Notifier.class), Set.copyOf(fields));
    }

    @Test
    void the_rules_live_in_specifications_not_in_the_service() {
        String service = code("dojo.domain.EnrolmentService");
        for (String detail : List.of("credits", "points", "static ", "new InMemory", "new GradebookTranscripts")) {
            assertFalse(service.contains(detail), "EnrolmentService should not mention " + detail.trim());
        }
    }

    @Test
    void the_domain_depends_on_nothing_outside_it() {
        List<String> domain = allClasses().stream().filter(name -> name.startsWith("dojo.domain.")).toList();
        assertTrue(domain.size() >= 10, "expected the domain types and ports in dojo.domain");
        for (String name : domain) {
            String source = code(name);
            for (String banned : List.of("dojo.adapters", "dojo.legacy", "LegacyGradebook", "InMemory", "System.out")) {
                assertFalse(source.contains(banned), name + " should not mention " + banned);
            }
        }
        for (String name : allClasses()) {
            boolean allowed = name.startsWith("dojo.adapters.") || name.startsWith("dojo.legacy.") || name.equals("dojo.App");
            if (!allowed) {
                assertFalse(code(name).contains("LegacyGradebook"), name + " should not know the gradebook");
            }
        }
        type("dojo.adapters.GradebookTranscripts");
    }
}
