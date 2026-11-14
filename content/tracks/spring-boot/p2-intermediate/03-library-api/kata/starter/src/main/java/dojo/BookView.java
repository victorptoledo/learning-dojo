package dojo;

/** A book as the API shows it. */
public record BookView(String isbn, String title, int year, String author) {
    public static BookView of(Book book) {
        return new BookView(book.getIsbn(), book.getTitle(), book.getYear(), book.getAuthor().getName());
    }
}
