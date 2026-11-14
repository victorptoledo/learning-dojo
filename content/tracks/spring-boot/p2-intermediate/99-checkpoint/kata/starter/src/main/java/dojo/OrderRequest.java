package dojo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OrderRequest(@NotEmpty List<@Valid Line> lines) {
    public record Line(@NotBlank String sku, @Min(1) int quantity) {}
}
