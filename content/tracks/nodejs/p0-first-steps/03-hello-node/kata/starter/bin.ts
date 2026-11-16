// The entry point: connects main() to the real process. You do not need to change it.
//   node bin.ts --lang pt --times 2 Ada Linus
import { main } from "./cli.ts";

process.exitCode = main(process.argv.slice(2), {
  env: process.env,
  stdout: (line) => process.stdout.write(`${line}\n`),
  stderr: (line) => process.stderr.write(`${line}\n`),
});
