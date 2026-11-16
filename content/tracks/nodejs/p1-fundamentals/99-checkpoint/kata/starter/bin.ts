// The entry point: node bin.ts [--json] [--min-size BYTES] <folder>. You do not need to change it.
import { main } from "./dupes.ts";

process.exitCode = await main(process.argv.slice(2), {
  stdout: (line) => process.stdout.write(`${line}\n`),
  stderr: (line) => process.stderr.write(`${line}\n`),
});
