package dojo;

import java.io.IOException;
import java.nio.file.Path;

/** Reads every timesheet in an inbox folder and writes the minutes per person and ISO week. */
public final class TimesheetImport {
    private TimesheetImport() {}

    public static ImportResult run(Path inbox, Path report) throws IOException {
        throw new UnsupportedOperationException("write TimesheetImport.run");
    }
}
