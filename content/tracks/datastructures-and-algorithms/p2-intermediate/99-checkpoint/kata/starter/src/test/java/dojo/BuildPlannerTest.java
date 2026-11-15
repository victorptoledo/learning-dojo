package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class BuildPlannerTest {
    @Test
    void the_example_of_the_readme() {
        var plan = new BuildPlanner();
        plan.addTask("compile", 30, List.of());
        plan.addTask("test", 60, List.of("compile"));
        plan.addTask("docs", 20, List.of("compile"));
        plan.addTask("package", 10, List.of("test", "docs"));
        assertEquals(List.of("compile", "docs", "test", "package"), plan.order());
        assertEquals(90, plan.earliestFinish("test"));
        assertEquals(100, plan.totalTime());
        assertEquals(List.of("compile", "test", "package"), plan.criticalPath());
        assertEquals(new TreeSet<>(List.of("docs", "package", "test")), plan.affectedBy("compile"));
        assertEquals(Optional.empty(), plan.findCycle());
    }
}
