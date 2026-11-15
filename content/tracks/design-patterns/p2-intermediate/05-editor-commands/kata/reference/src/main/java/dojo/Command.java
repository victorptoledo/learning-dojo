package dojo;

/**
 * An edit as an object: it can be run, undone after running, and described. A command that
 * throws from {@code execute} must leave the document unchanged.
 */
public interface Command {
    void execute(Document document);

    void undo(Document document);

    String describe();
}
