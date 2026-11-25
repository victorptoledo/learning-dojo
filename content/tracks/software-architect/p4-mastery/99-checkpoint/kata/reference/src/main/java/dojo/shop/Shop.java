package dojo.shop;

import dojo.shop.checkout.Checkout;
import dojo.shop.pricing.migration.StranglerPricing;
import dojo.shop.reports.SalesReport;

public record Shop(Checkout checkout, StranglerPricing pricing, SalesReport report) {}
