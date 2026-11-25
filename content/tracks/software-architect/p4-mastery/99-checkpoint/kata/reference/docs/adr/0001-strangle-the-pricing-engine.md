# 0001 Strangle the legacy pricing engine behind a port

## Status

Accepted

## Context

Checkout calls the 2009 `PriceEngine` directly, with string codes for categories and customer
types. Every invoice depends on its integer rounding, nobody dares to change it, and new pricing
rules (promotions, other currencies) cannot be added. A big-bang rewrite would have to match
years of behaviour on the first day.

## Decision

We will put pricing behind a `Pricing` port and keep the legacy engine as one adapter of it
(branch by abstraction). A modern implementation grows next to it, and a strangler routes each
category to the legacy engine, to a shadow run that compares both and keeps the legacy answer,
or to the modern implementation. A category moves to modern only after a clean shadow run.
Fitness functions keep the legacy engine out of every class but its adapter, with a ratchet for
the report that still uses it.

## Consequences

Categories migrate one at a time, and going back to legacy is a configuration change. Shadow
runs cost a second calculation per price and a mismatch log someone must read. The legacy
adapter and the strangler are temporary code that we must remove when the last category has
moved, and the sales report stays in the baseline until it is migrated.
