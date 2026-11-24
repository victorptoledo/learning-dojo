package dojo.domain;

/** The use case: it gathers what the rules need, runs them and records the enrolment. */
public final class EnrolmentService {
    private final Students students;
    private final Enrolments enrolments;
    private final Transcripts transcripts;
    private final Notifier notifier;

    public EnrolmentService(Students students, Enrolments enrolments, Transcripts transcripts, Notifier notifier) {
        this.students = students;
        this.enrolments = enrolments;
        this.transcripts = transcripts;
        this.notifier = notifier;
    }

    public String enrol(String studentId, Course course) {
        Student student = students.find(studentId)
                .orElseThrow(() -> new IllegalArgumentException("unknown student: " + studentId));
        Applicant applicant = new Applicant(student, transcripts.of(studentId), enrolments.coursesOf(studentId));
        for (Requirement requirement : EnrolmentRules.forCourse(course)) {
            if (!requirement.rule().isSatisfiedBy(applicant)) {
                return "refused: " + requirement.reason();
            }
        }
        if (enrolments.countIn(course.code()) >= course.capacity()) {
            return "refused: course full";
        }
        enrolments.add(studentId, course.code());
        notifier.send(student, "enrolled in " + course.code());
        return "enrolled";
    }
}
