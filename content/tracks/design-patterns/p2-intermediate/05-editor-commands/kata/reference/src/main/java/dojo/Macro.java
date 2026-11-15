package dojo;

import java.util.ArrayList;
import java.util.List;

/** Composite: several commands that run, and undo, as one. */
public final class Macro implements Command {
    private final String name;
    private final List<Command> steps;

    public Macro(String name, List<Command> steps) {
        this.name = name;
        this.steps = List.copyOf(steps);
    }

    @Override
    public void execute(Document document) {
        List<Command> done = new ArrayList<>();
        try {
            for (Command step : steps) {
                step.execute(document);
                done.add(step);
            }
        } catch (RuntimeException e) {
            for (Command step : done.reversed()) {
                step.undo(document);
            }
            throw e;
        }
    }

    @Override
    public void undo(Document document) {
        for (Command step : steps.reversed()) {
            step.undo(document);
        }
    }

    @Override
    public String describe() {
        return name;
    }
}
