package dojo.app.core;

import java.util.Optional;

public interface ItemRepository {
    Optional<Item> find(String id);

    void save(Item item);
}
