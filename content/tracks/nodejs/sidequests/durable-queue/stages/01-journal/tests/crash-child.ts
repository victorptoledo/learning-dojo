// Run by the crash test as a separate process: adds three jobs, takes one, says so on stdout
// and is killed with SIGKILL, so nothing gets a chance to clean up.
import { openQueue } from "../queue.ts";

const dir = process.argv[2] ?? "";
const queue = await openQueue(dir);
await queue.add("email", { to: "ana@example.com" });
await queue.add("email", { to: "bia@example.com" }, { priority: 5 });
await queue.add("report", { month: 3 });
const taken = await queue.take();
process.stdout.write(`took ${taken?.id}\n`, () => {
  process.kill(process.pid, "SIGKILL");
});
