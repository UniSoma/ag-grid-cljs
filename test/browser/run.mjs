// Playwright driver for the committed browser suite (ADR 0015).
//
// The ASSERTIONS live in cljs.test, inside the compiled :browser-test build
// (run `shadow-cljs compile browser-test` first). This script is the DRIVER
// only: it serves target/browser-test over HTTP, launches headless Chromium,
// services real mouse gestures the suite requests, reads the cljs.test summary
// the runner-ns stashes on window.__agTestSummary, and enforces the
// unexpected-console-error tripwire (known AG Grid license errors allowlisted).
//
// Exit 0 iff every test passed and no unexpected console error surfaced.

import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { extname, join, normalize } from "node:path";
import { chromium } from "playwright";

const TEST_DIR = "target/browser-test";
const RUN_TIMEOUT_MS = 60_000;

// AG Grid's no-license console errors (watermark banner + license notices).
// Anything NOT matching these fails the run — the regression tripwire. Kept
// scoped to AG Grid's known Enterprise-license wording so an unrelated error
// that merely contains "license" is not swallowed (ADR 0015 §4).
const LICENSE_ALLOWLIST = [
  /ag grid enterprise/i,
  /enterprise license/i,
  /license key/i,
  /watermark/i,
  /\*{5,}/, // the asterisk banner box
];

const MIME = {
  ".html": "text/html",
  ".js": "text/javascript",
  ".map": "application/json",
  ".css": "text/css",
  ".json": "application/json",
};

function serve(root) {
  const server = createServer(async (req, res) => {
    try {
      const url = decodeURIComponent(req.url.split("?")[0]);
      const rel = normalize(url === "/" ? "/index.html" : url).replace(/^(\.\.[/\\])+/, "");
      const body = await readFile(join(root, rel));
      res.writeHead(200, { "content-type": MIME[extname(rel)] || "application/octet-stream" });
      res.end(body);
    } catch {
      res.writeHead(404);
      res.end("not found");
    }
  });
  return new Promise((resolve) => server.listen(0, "127.0.0.1", () => resolve(server)));
}

async function performGesture(page, g) {
  if (g.type === "wheel") {
    const box = await page.locator(g.selector).first().boundingBox();
    if (box) {
      await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
      await page.mouse.wheel(0, g.dy || 300);
    }
  }
}

// Loop until the suite is done, servicing any gesture it asks for meanwhile.
async function driveUntilDone(page) {
  for (;;) {
    await page.waitForFunction(
      () => (window.__agTestSummary && window.__agTestSummary.done) || !!window.__agGesture,
      { timeout: RUN_TIMEOUT_MS },
    );
    const gesture = await page.evaluate(() => {
      const g = window.__agGesture;
      window.__agGesture = null;
      return g;
    });
    if (gesture) {
      await performGesture(page, gesture);
      await page.evaluate(() => window.__agGestureDone && window.__agGestureDone(true));
      continue;
    }
    return page.evaluate(() => window.__agTestSummary);
  }
}

function isAllowlisted(text) {
  return LICENSE_ALLOWLIST.some((re) => re.test(text));
}

async function main() {
  const server = await serve(TEST_DIR);
  const { port } = server.address();
  const browser = await chromium.launch();
  const page = await browser.newPage();

  const unexpectedErrors = [];
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      const text = msg.text();
      if (!isAllowlisted(text)) unexpectedErrors.push(text);
    }
  });
  page.on("pageerror", (err) => {
    const text = String(err);
    if (!isAllowlisted(text)) unexpectedErrors.push(text);
  });

  let summary, failure;
  try {
    await page.goto(`http://127.0.0.1:${port}/`, { waitUntil: "load" });
    summary = await driveUntilDone(page);
  } catch (e) {
    failure = e;
  } finally {
    await browser.close();
    server.close();
  }

  if (failure) {
    console.error(`\nBrowser suite did not finish: ${failure.message}`);
    process.exit(1);
  }

  console.log(
    `\nBrowser suite: ${summary.test} tests, ${summary.pass} pass, ` +
      `${summary.fail} fail, ${summary.error} error.`,
  );

  let ok = true;
  if (summary.failures > 0) {
    console.error(`FAIL: ${summary.failures} failing assertion(s) (see cljs.test output above).`);
    ok = false;
  }
  if (unexpectedErrors.length > 0) {
    console.error(`FAIL: ${unexpectedErrors.length} unexpected console error(s):`);
    for (const e of unexpectedErrors) console.error(`  - ${e}`);
    ok = false;
  }
  process.exit(ok ? 0 : 1);
}

main();
