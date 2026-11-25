package dojo.shop;

import dojo.shop.checkout.Checkout;
import dojo.shop.reports.SalesReport;

public record Shop(Checkout checkout, SalesReport report) {}
