// Playwright driver for the ADR 0018 callback-bean browser benchmarks
// (`bb bench-browser`). Serves the :browser-bench release build (compile it
// first), launches headless Chromium, echoes the page's console output, and
// exits when the page sets window.__benchDone. Measurements are recorded in
// docs/research/key-transform-benchmarks.md.

import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { extname, join, normalize } from "node:path";
import { chromium } from "playwright";

const DIR = "target/browser-bench";
const RUN_TIMEOUT_MS = 300_000;
const INDEX =
  '<!doctype html><html><head><meta charset="utf-8"><title>bench</title></head>' +
  '<body><script src="/js/main.js"></script></body></html>';

const MIME = { ".html": "text/html", ".js": "text/javascript", ".map": "application/json" };

const server = createServer(async (req, res) => {
  const url = decodeURIComponent(req.url.split("?")[0]);
  if (url === "/") {
    res.writeHead(200, { "content-type": "text/html" });
    res.end(INDEX);
    return;
  }
  try {
    const rel = normalize(url).replace(/^(\.\.[/\\])+/, "");
    const body = await readFile(join(DIR, rel));
    res.writeHead(200, { "content-type": MIME[extname(rel)] || "application/octet-stream" });
    res.end(body);
  } catch {
    res.writeHead(404);
    res.end("not found");
  }
});

await new Promise((resolve) => server.listen(0, "127.0.0.1", () => resolve()));
const { port } = server.address();
const browser = await chromium.launch({ args: ["--js-flags=--expose-gc"] });
const page = await browser.newPage();
page.on("console", (msg) => console.log(msg.text()));
page.on("pageerror", (err) => console.error(`pageerror: ${err}`));

let ok = true;
try {
  await page.goto(`http://127.0.0.1:${port}/`, { waitUntil: "load" });
  await page.waitForFunction(() => window.__benchDone, { timeout: RUN_TIMEOUT_MS });
} catch (e) {
  console.error(`Benchmark run did not finish: ${e.message}`);
  ok = false;
} finally {
  await browser.close();
  server.close();
}
process.exit(ok ? 0 : 1);
