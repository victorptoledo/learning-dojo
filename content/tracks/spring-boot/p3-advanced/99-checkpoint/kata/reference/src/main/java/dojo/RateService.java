package dojo;

import java.math.BigDecimal;
import java.time.Clock;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RateService {
    private final RateRepository rates;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public RateService(RateRepository rates, Clock clock, ApplicationEventPublisher events) {
        this.rates = rates;
        this.clock = clock;
        this.events = events;
    }

    @Cacheable("rates")
    @Transactional(readOnly = true)
    public RateView find(String code) {
        return rates.findById(code).map(RateView::of).orElseThrow(() -> new RateNotFoundException(code));
    }

    @PreAuthorize("hasRole('TRADER')")
    @CacheEvict(cacheNames = "rates", key = "#code")
    @Transactional
    public RateView update(String code, BigDecimal perEuro) {
        String by = SecurityContextHolder.getContext().getAuthentication().getName();
        Rate rate = rates.findById(code).orElse(null);
        BigDecimal previous = null;
        if (rate == null) {
            rate = rates.save(new Rate(code, perEuro, clock.instant()));
        } else {
            previous = rate.getPerEuro();
            rate.change(perEuro, clock.instant());
        }
        events.publishEvent(new RateChanged(code, previous, perEuro, by));
        return RateView.of(rate);
    }
}
