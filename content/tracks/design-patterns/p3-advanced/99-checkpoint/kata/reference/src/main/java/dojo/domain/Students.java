package dojo.domain;

import java.util.Optional;

/** Driven port: where students are kept. */
public interface Students {
    Optional<Student> find(String id);
}
