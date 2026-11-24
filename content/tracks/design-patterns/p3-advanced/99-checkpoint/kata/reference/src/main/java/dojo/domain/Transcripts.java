package dojo.domain;

import java.util.List;

/** Driven port: a student's grades, in the domain's terms. */
public interface Transcripts {
    List<Grade> of(String studentId);
}
