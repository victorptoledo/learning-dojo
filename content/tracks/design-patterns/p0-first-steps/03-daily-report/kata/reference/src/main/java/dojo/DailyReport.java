package dojo;

import java.time.Clock;
import java.time.LocalDate;

/** Wires the pieces: asks the source for today's sales, totals them and formats them. */
public class DailyReport {
    private final SaleSource source;
    private final Clock clock;

    public DailyReport(SaleSource source, Clock clock) {
        this.source = source;
        this.clock = clock;
    }

    /** The production wiring: the real archive and the system clock. */
    public static DailyReport withDefaults() {
        return new DailyReport(new SaleArchive(), Clock.systemDefaultZone());
    }

    public String render() {
        LocalDate today = LocalDate.now(clock);
        return TextReport.format(DailyTotals.of(today, source.salesOn(today)));
    }

    public static void main(String[] args) {
        System.out.println(withDefaults().render());
    }
}
