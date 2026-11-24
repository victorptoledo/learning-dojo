package dojo.domain;

import dojo.legacy.LegacyGradebook;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enrols students in courses. Everything lives here: the students, the enrolments, the
 * gradebook and the mail, all static, so nothing can be replaced in a test.
 */
public class EnrolmentService {
    /** student id -> {name, credits} */
    public static final Map<String, Object[]> STUDENTS = new HashMap<>();
    /** student id -> course codes */
    public static final Map<String, List<String>> ENROLMENTS = new HashMap<>();
    public static LegacyGradebook GRADEBOOK = new LegacyGradebook();

    public String enrol(String studentId, String course, int capacity, int minCredits, String prerequisite) {
        Object[] student = STUDENTS.get(studentId);
        if (student == null) {
            throw new IllegalArgumentException("unknown student: " + studentId);
        }
        int credits = (Integer) student[1];
        if (credits < minCredits) {
            return "refused: needs " + minCredits + " credits";
        }
        if (prerequisite != null) {
            boolean passed = false;
            for (String entry : GRADEBOOK.transcript(studentId).split(";")) {
                String[] parts = entry.split(":");
                if (parts.length == 2 && parts[0].trim().equals(prerequisite) && !parts[1].trim().startsWith("F")) {
                    passed = true;
                }
            }
            if (!passed) {
                return "refused: needs " + prerequisite;
            }
        }
        List<String> mine = ENROLMENTS.computeIfAbsent(studentId, id -> new ArrayList<>());
        if (mine.size() >= 5) {
            return "refused: too many courses";
        }
        if (mine.contains(course)) {
            return "refused: already enrolled";
        }
        long taken = ENROLMENTS.values().stream().filter(courses -> courses.contains(course)).count();
        if (taken >= capacity) {
            return "refused: course full";
        }
        mine.add(course);
        System.out.println("MAIL " + student[0] + ": enrolled in " + course);
        return "enrolled";
    }
}
