package dojo;

/** Every kind of discount the shop offers. */
public sealed interface Discount permits PercentOff, AmountOff, BuyXGetY {}
