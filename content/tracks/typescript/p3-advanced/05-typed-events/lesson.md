# Typed Events: A Type-Safe Event Emitter

Build an event emitter whose type parameter maps event names to payloads, so unknown events and
wrong payloads do not compile: generic methods with `K extends keyof Events`, a mapped type with
key remapping for `onAll`, and a `waitFor` promise with `AbortSignal`. Runtime and type-level
tests check both sides. Press **Enter** to create the kata folder and open it.
