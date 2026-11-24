package dojo.adapters;

import dojo.domain.Student;
import dojo.domain.Students;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryStudents implements Students {
    private final Map<String, Student> byId = new LinkedHashMap<>();

    public void add(Student student) {
        if (byId.putIfAbsent(student.id(), student) != null) {
            throw new IllegalArgumentException("student already exists: " + student.id());
        }
    }

    @Override
    public Optional<Student> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }
}
