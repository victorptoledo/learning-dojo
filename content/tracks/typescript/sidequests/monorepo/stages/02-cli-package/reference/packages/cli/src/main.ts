// The command itself: node packages/cli/src/main.ts sum ledger.txt
import { readFileSync } from "node:fs";
import { run } from "./run.ts";

process.exitCode = run(process.argv.slice(2), {
  readFile: (path) => readFileSync(path, "utf8"),
  out: (line) => console.log(line),
  err: (line) => console.error(line),
});
