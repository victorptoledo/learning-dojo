package dojo.domain;

/** Driven port: told about every committed transfer. */
@FunctionalInterface
public interface TransferLog {
    void record(Transfer transfer);
}
