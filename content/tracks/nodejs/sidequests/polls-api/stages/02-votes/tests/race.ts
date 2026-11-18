// Races requests against each other: every request sends its headers first, and only when all
// of them are open do the bodies go out. A server that checks something before the body is read
// and changes it after lets all of them through.
import { request } from "node:http";
import { setTimeout as sleep } from "node:timers/promises";

export interface RaceReply {
  status: number;
  body: string;
}

export async function race(
  base: string,
  method: string,
  path: string,
  requests: { headers: Record<string, string>; body: string }[],
): Promise<RaceReply[]> {
  const url = new URL(path, base);
  const pending = requests.map(({ headers, body }) => {
    const req = request(url, { method, headers: { ...headers, "content-length": String(Buffer.byteLength(body)) }, agent: false });
    const reply = new Promise<RaceReply>((resolve, reject) => {
      req.on("response", (res) => {
        let text = "";
        res.setEncoding("utf8");
        res.on("data", (chunk: string) => (text += chunk));
        res.on("end", () => resolve({ status: res.statusCode ?? 0, body: text }));
        res.on("error", reject);
      });
      req.on("error", reject);
    });
    req.flushHeaders();
    return { req, body, reply };
  });
  await sleep(150);
  for (const { req, body } of pending) req.end(body);
  return Promise.all(pending.map(({ reply }) => reply));
}
