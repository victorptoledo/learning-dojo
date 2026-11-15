package dojo;

/** Remembers the text before the replacement: replacing is not reversible in general. */
public final class ReplaceAll implements Command {
    private final String target;
    private final String replacement;
    private String before;

    public ReplaceAll(String target, String replacement) {
        this.target = target;
        this.replacement = replacement;
    }

    @Override
    public void execute(Document document) {
        before = document.text();
        document.replaceWith(before.replace(target, replacement));
    }

    @Override
    public void undo(Document document) {
        document.replaceWith(before);
    }

    @Override
    public String describe() {
        return "replace " + target + "->" + replacement;
    }
}
