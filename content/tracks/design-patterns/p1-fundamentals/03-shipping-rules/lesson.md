# Shipping Rules

The Strategy refactoring you will do most often in real code: a method with one branch per
shipping method becomes a set of `ShippingRule` lambdas, a small factory that names them and a
calculator that only looks them up. The visible tests pin every price and pass before you
start, so you can move one branch at a time and stay green. Press **Enter** to create the kata
folder and open it.
