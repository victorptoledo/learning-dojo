package dojo;

/** The availability SLI over the window and what it means for the error budget. */
public record SloReport(long total, long good, double availability, double burnRate, double budgetRemaining,
                        boolean paging) {}
