package dojo;

import java.time.LocalDate;

/** One line of the till: a product sold on a day, for an amount in cents. */
public record Sale(String product, LocalDate day, long cents) {}
