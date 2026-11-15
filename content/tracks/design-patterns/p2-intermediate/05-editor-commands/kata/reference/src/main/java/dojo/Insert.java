package dojo;

public record Insert(int at, String fragment) implements Command {
    @Override
    public void execute(Document document) {
        document.insert(at, fragment);
    }

    @Override
    public void undo(Document document) {
        document.delete(at, fragment.length());
    }

    @Override
    public String describe() {
        return "insert " + fragment.length();
    }
}
