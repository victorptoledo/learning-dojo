package dojo.library;

import dojo.library.adapters.InMemoryBooks;
import dojo.library.application.BorrowBook;
import dojo.library.application.ReturnBook;

/** The wired library. Do not change it. */
public record Library(BorrowBook borrowBook, ReturnBook returnBook, InMemoryBooks books) {}
