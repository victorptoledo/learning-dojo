package dojo.domain;

import java.util.function.Supplier;

/** Driven port: the unit of work. Everything saved inside {@code work} commits together or not at all. */
public interface Transactions {
    <T> T inTransaction(Supplier<T> work);
}
