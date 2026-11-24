package dojo.domain;

import java.util.List;

/** Driven port: who takes which course. */
public interface Enrolments {
    List<String> coursesOf(String studentId);

    int countIn(String course);

    void add(String studentId, String course);
}
