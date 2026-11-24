# Course Enrolment

`dojo.domain.EnrolmentService` decides who may take a course. It keeps the students and the
enrolments in public static maps, parses the registrar's 2006 gradebook strings inline, hides
five business rules in one `if` ladder and prints its mail to the console. Nothing in it can
be replaced in a test, and every new rule means editing the ladder.

Use this phase's patterns together:

- **repositories** behind **ports** the domain defines in its own terms;
- **specifications** for the rules, so each one is named, tested alone and combined;
- an **anti-corruption layer** that turns the gradebook's strings into grade points;
- **constructor injection**, with one **composition root** (`dojo.App`) that is the only place
  knowing both the domain and the adapters.

`dojo.legacy.LegacyGradebook` is a library: do not change it.

## The target design

| Type | What it does |
| --- | --- |
| `domain.Student` | record `(String id, String name, int credits)` |
| `domain.Course` | record `(String code, int capacity, int minCredits, String prerequisite)`; `prerequisite` is `null` when there is none. Validates: blank code → `"course code is required"`, capacity below 1, negative credits (all `IllegalArgumentException`) |
| `domain.Grade` | record `(String course, double points)` |
| `domain.Applicant` | record `(Student student, List<Grade> transcript, List<String> enrolments)`: what the rules look at |
| `domain.Specification<T>` | functional interface `boolean isSatisfiedBy(T candidate)`, with default `and`, `or` and `not()` |
| `domain.Requirement` | record `(Specification<Applicant> rule, String reason)` |
| `domain.EnrolmentRules` | `MAX_COURSES = 5`; `hasCredits(int min)`, `passed(String course)` (a grade above 0.0 in that course), `takingFewerThan(int courses)`, `enrolledIn(String course)`; `List<Requirement> forCourse(Course)` |
| port `domain.Students` | `Optional<Student> find(String id)` |
| port `domain.Enrolments` | `List<String> coursesOf(String studentId)`, `int countIn(String course)`, `void add(String studentId, String course)` |
| port `domain.Transcripts` | `List<Grade> of(String studentId)` (its only method) |
| port `domain.Notifier` | `void send(Student student, String message)` (its only method) |
| `domain.EnrolmentService` | `EnrolmentService(Students, Enrolments, Transcripts, Notifier)`; `String enrol(String studentId, Course course)` |
| `adapters.InMemoryStudents implements Students` | plus `add(Student)` |
| `adapters.InMemoryEnrolments implements Enrolments` | |
| `adapters.GradebookTranscripts implements Transcripts` | `GradebookTranscripts(LegacyGradebook)`: the anti-corruption layer |
| `adapters.ConsoleNotifier implements Notifier` | prints `MAIL <name>: <message>` |
| `dojo.App` | `static EnrolmentService enrolmentService(InMemoryStudents, InMemoryEnrolments, LegacyGradebook)` |

`enrol` keeps today's behaviour:

- An unknown student throws `IllegalArgumentException("unknown student: " + id)`.
- `forCourse` lists the requirements in this order, and the first one not met is the answer
  `"refused: " + reason`: `needs <min> credits` (exactly the minimum is enough), `needs <code>`
  (only when the course has a prerequisite), `too many courses` (five or more already),
  `already enrolled`.
- After them, a course with as many students as its capacity answers `"refused: course full"`.
- Otherwise the enrolment is added, the student is told `"enrolled in " + code`, and the answer
  is `"enrolled"`. A refusal records nothing and tells nobody.

The gradebook writes a transcript as `"MATH101:B+;PHYS201:C"`. Letters are worth A 4.0, B 3.0,
C 2.0, D 1.0 and F 0.0; a `+` adds 0.3 and a `-` takes 0.3 off, but A+ stays 4.0 and an F is
always 0.0. Trim spaces around codes and letters, and skip what cannot be read: empty entries,
entries without exactly one colon, and letters outside A–F.

## What the grading tests check

- Every rule, its order and its message; refusals leave no trace; a full course.
- `and`, `or` and `not`, and each rule of `EnrolmentRules` on its own.
- The translation of messy gradebook rows.
- The composition root, end to end with the real gradebook.
- The dependency rule, by reading your sources: no class in `dojo.domain` mentions
  `dojo.adapters`, `dojo.legacy`, `LegacyGradebook`, `InMemory` or `System.out`; only the
  adapters, the legacy library and `dojo.App` know the gradebook. `EnrolmentService` holds its
  four ports and nothing else, has no static state, and mentions neither credits nor points:
  the rules live in `EnrolmentRules`.

The visible tests describe the target design, so they do not compile until the ports and
adapters exist. Start with the records and the ports, then move each rule into a specification.
