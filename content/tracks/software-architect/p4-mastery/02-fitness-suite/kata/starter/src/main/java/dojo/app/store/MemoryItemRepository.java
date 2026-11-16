package dojo.app.store;

import dojo.app.core.Item;
import dojo.app.core.ItemRepository;
import dojo.app.web.RequestLog;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MemoryItemRepository implements ItemRepository {
    private final Map<String, Item> items = new HashMap<>();

    @Override
    public Optional<Item> find(String id) {
        RequestLog.note("find " + id);
        return Optional.ofNullable(items.get(id));
    }

    @Override
    public void save(Item item) {
        items.put(item.id(), item);
    }
}
