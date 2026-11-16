# Design an Aggregate

An order with public setters has no rules: any code can add a line to a submitted order or set
the total to anything. Here the rules live in a service that checks some of them, in some
methods, and the order holds a whole `Customer` object it never needs. You will make `Order` an
**aggregate root**: the only way in is through methods that keep its invariants, money and ids
become **value objects**, the customer is referenced by id, and what happened is recorded as
**domain events**. Press **Enter** to create the kata folder and open it.
