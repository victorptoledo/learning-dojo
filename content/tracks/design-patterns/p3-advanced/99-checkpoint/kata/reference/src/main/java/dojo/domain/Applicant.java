package dojo.domain;

import java.util.List;

/** What the enrolment rules look at: the student, their grades and their current courses. */
public record Applicant(Student student, List<Grade> transcript, List<String> enrolments) {
    public Applicant {
        transcript = List.copyOf(transcript);
        enrolments = List.copyOf(enrolments);
    }
}
