package dojo.domain;

import java.util.ArrayList;
import java.util.List;

/** Every enrolment rule, as specifications; the service only runs them. */
public final class EnrolmentRules {
    public static final int MAX_COURSES = 5;

    private EnrolmentRules() {}

    public static Specification<Applicant> hasCredits(int min) {
        return applicant -> applicant.student().credits() >= min;
    }

    /** A grade better than F in the course. */
    public static Specification<Applicant> passed(String course) {
        return applicant -> applicant.transcript().stream()
                .anyMatch(grade -> grade.course().equals(course) && grade.points() > 0);
    }

    public static Specification<Applicant> takingFewerThan(int courses) {
        return applicant -> applicant.enrolments().size() < courses;
    }

    public static Specification<Applicant> enrolledIn(String course) {
        return applicant -> applicant.enrolments().contains(course);
    }

    /** The requirements of a course, in the order their reasons are reported. */
    public static List<Requirement> forCourse(Course course) {
        List<Requirement> requirements = new ArrayList<>();
        requirements.add(new Requirement(hasCredits(course.minCredits()), "needs " + course.minCredits() + " credits"));
        if (course.prerequisite() != null) {
            requirements.add(new Requirement(passed(course.prerequisite()), "needs " + course.prerequisite()));
        }
        requirements.add(new Requirement(takingFewerThan(MAX_COURSES), "too many courses"));
        requirements.add(new Requirement(enrolledIn(course.code()).not(), "already enrolled"));
        return List.copyOf(requirements);
    }
}
