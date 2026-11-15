package dojo;

/** Remembers what it removed, so undo can put exactly that back. */
public final class Delete implements Command {
    private final int at;
    private final int length;
    private String removed;

    public Delete(int at, int length) {
        this.at = at;
        this.length = length;
    }

    @Override
    public void execute(Document document) {
        removed = document.delete(at, length);
    }

    @Override
    public void undo(Document document) {
        document.insert(at, removed);
    }

    @Override
    public String describe() {
        return "delete " + length;
    }
}
