package dojo;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rates")
public class RateController {
    private final RateService rates;

    public RateController(RateService rates) {
        this.rates = rates;
    }

    @GetMapping("/{code}")
    public RateView get(@PathVariable String code) {
        return rates.find(code);
    }

    @PutMapping("/{code}")
    public RateView put(@PathVariable String code, @Valid @RequestBody NewRate body) {
        return rates.update(code, body.perEuro());
    }
}
