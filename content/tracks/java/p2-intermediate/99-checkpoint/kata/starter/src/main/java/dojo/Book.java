package dojo;

public record Book(String isbn, String title, String author) implements Identified<String> {
    @Override
    public String id() {
        return isbn;
    }
}
