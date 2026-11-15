# Payment Gateway

Checkout talks to a 2011 payment SDK in its own language and mixes retries and counters into
the same method. You will hide the SDK behind an **adapter**, then pull retrying and metering
out into **decorators** of one `PaymentGateway` interface, and see why their order matters.
Press **Enter** to create the kata folder and open it.
