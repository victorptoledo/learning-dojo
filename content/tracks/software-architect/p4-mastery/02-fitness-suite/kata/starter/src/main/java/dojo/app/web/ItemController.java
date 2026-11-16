package dojo.app.web;

import dojo.app.core.Item;
import dojo.app.store.MemoryItemRepository;

public class ItemController {
    private final MemoryItemRepository repository = new MemoryItemRepository();

    public String get(String id) {
        return repository.find(id).map(Item::name).orElse("404");
    }
}
