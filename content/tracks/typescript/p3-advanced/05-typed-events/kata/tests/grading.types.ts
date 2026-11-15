import type { Handlers } from "../events.ts";
import { TypedEmitter } from "../events.ts";
import type { Equal, Expect } from "./expect.ts";

type User = { name: string };
type Events = { login: User; logout: undefined; tick: number };
const emitter = new TypedEmitter<Events>();

// test: listeners receive the payload type of their event
emitter.on("login", (user) => {
  const name: string = user.name;
  return name;
});
emitter.once("tick", (n) => n.toFixed(1));

// test: unknown event names do not compile
// @ts-expect-error: "signup" is not an event
emitter.on("signup", () => {});
// @ts-expect-error: "signup" is not an event
emitter.emit("signup", 1);

// test: wrong payloads do not compile
// @ts-expect-error: a tick carries a number
emitter.emit("tick", "1");
// @ts-expect-error: a login carries a User
emitter.emit("login", { id: 1 });

// test: waitFor resolves with the payload type
export const next = emitter.waitFor("tick");
export type Next = Expect<Equal<typeof next, Promise<number>>>;

// test: Handlers has one optional handler per event
export type UserHandlers = Expect<
  Equal<
    Handlers<Events>,
    {
      onLogin?: (payload: User) => void;
      onLogout?: (payload: undefined) => void;
      onTick?: (payload: number) => void;
    }
  >
>;
// @ts-expect-error: there is no "signup" event
emitter.onAll({ onSignup: () => {} });
