package dojo.shop;

import dojo.shop.sales.SalesService;
import dojo.shop.warehouse.WarehouseService;

/** The wired shop. Do not change it. */
public record Shop(SalesService sales, WarehouseService warehouse) {}
