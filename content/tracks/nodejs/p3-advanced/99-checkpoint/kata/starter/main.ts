// Runs a manifest from the command line (npm start -- manifest.json [root]). You do not need
// to change it.
//   echo '{"jobs":[{"id":"hi","kind":"tool","tool":"echo","args":["hello"]}]}' > jobs.json
//   npm start -- jobs.json .
import { readFile } from "node:fs/promises";
import { ManifestError, parseManifest, runJobs } from "./runner.ts";

const [manifestFile, root = "."] = process.argv.slice(2);
if (manifestFile === undefined) {
  console.error("usage: node main.ts <manifest.json> [root]");
  process.exit(2);
}
try {
  const manifest = parseManifest(await readFile(manifestFile, "utf8"));
  const results = await runJobs(manifest, { root });
  for (const result of results) {
    console.log(JSON.stringify(result));
  }
  process.exitCode = results.every((each) => each.ok) ? 0 : 1;
} catch (error) {
  if (!(error instanceof ManifestError)) {
    throw error;
  }
  console.error(`invalid manifest: ${error.message}`);
  process.exitCode = 2;
}
