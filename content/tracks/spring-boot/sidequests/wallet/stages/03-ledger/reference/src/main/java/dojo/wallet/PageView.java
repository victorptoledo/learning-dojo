package dojo.wallet;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** A page in the shape of the API, independent of Spring Data. */
public record PageView<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    static <E, T> PageView<T> of(Page<E> page, Function<E, T> view) {
        return new PageView<>(page.getContent().stream().map(view).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
