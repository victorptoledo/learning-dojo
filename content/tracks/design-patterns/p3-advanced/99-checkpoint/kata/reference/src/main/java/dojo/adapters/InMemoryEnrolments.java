package dojo.adapters;

import dojo.domain.Enrolments;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InMemoryEnrolments implements Enrolments {
    private final Map<String, List<String>> byStudent = new LinkedHashMap<>();

    @Override
    public List<String> coursesOf(String studentId) {
        return List.copyOf(byStudent.getOrDefault(studentId, List.of()));
    }

    @Override
    public int countIn(String course) {
        return (int) byStudent.values().stream().filter(courses -> courses.contains(course)).count();
    }

    @Override
    public void add(String studentId, String course) {
        byStudent.computeIfAbsent(studentId, id -> new ArrayList<>()).add(course);
    }
}
