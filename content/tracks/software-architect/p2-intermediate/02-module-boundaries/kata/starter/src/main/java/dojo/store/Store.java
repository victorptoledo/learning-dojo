package dojo.store;

import dojo.store.catalog.api.Catalog;
import dojo.store.orders.api.Orders;
import dojo.store.shipping.api.Shipping;

/** The wired store, as other code sees it: three apis. Do not change it. */
public record Store(Catalog catalog, Orders orders, Shipping shipping) {}
