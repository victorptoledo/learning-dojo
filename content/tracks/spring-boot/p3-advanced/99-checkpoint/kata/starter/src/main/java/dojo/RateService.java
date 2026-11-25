package dojo;

import java.math.BigDecimal;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * It works, but none of the rules of the README are here yet: every read goes to the database,
 * anyone logged in can change a rate, and nobody hears about the change.
 */
@Service
public class RateService {
    private final RateRepository rates;
    private final Clock clock;

    public RateService(RateRepository rates, Clock clock) {
        this.rates = rates;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public RateView find(String code) {
        return rates.findById(code).map(RateView::of).orElseThrow(() -> new RateNotFoundException(code));
    }

    @Transactional
    public RateView update(String code, BigDecimal perEuro) {
        Rate rate = rates.findById(code).orElse(null);
        if (rate == null) {
            rate = rates.save(new Rate(code, perEuro, clock.instant()));
        } else {
            rate.change(perEuro, clock.instant());
        }
        return RateView.of(rate);
    }
}
