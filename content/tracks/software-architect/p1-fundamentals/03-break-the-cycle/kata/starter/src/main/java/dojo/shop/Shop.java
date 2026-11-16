package dojo.shop;

import dojo.shop.billing.InvoiceService;
import dojo.shop.billing.PaymentService;
import dojo.shop.orders.OrderRepository;
import dojo.shop.orders.OrderService;

/** The wired shop. Do not change it. */
public record Shop(OrderService orders, PaymentService payments, OrderRepository orderRepository,
                   InvoiceService invoices) {}
