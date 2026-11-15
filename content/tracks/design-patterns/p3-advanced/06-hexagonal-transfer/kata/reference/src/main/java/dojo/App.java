package dojo;

import dojo.adapters.ConsoleTransferLog;
import dojo.adapters.InMemoryBank;
import dojo.adapters.LegacyFxRates;
import dojo.domain.TransferService;
import dojo.legacy.LegacyFxFeed;

/** The composition root: the only place that knows both the domain and the adapters. */
public final class App {
    private App() {}

    public static TransferService transferService(InMemoryBank bank, LegacyFxFeed feed) {
        return new TransferService(bank, new LegacyFxRates(feed), bank, new ConsoleTransferLog());
    }
}
