// Key-registry codegen (ADR 0007). Manual — run on an AG Grid dependency bump:
//
//   cd tools/codegen && npm install && npm run generate
//
// Extracts the AG Grid option/event surface from the repo's installed
// ag-grid-community pin and emits two committed artifacts:
//
//   src/main/ag_grid_cljs/impl/registry.cljs   the goog.DEBUG-guarded CLJS literal
//   docs/reference/ag-grid-options.md           the human reference table
//
// GridOptions keys, event names and the event->handler map come from
// ag-grid-community's runtime constants; per-key type/JSDoc metadata and the
// ColDef/ColGroupDef key sets come from a ts-morph pass over the shipped .d.ts
// (typescript@5.x, embedded in ts-morph). Output is sorted, so the tool is
// deterministic and idempotent for a given pin.

import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import { writeFileSync, readFileSync } from 'node:fs';
import { Project } from 'ts-morph';

const require = createRequire(import.meta.url);
const here = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(here, '..', '..');

// --- locate the installed ag-grid-community pin -----------------------------

const agRoot = resolve(repoRoot, 'node_modules', 'ag-grid-community');
const agVersion = JSON.parse(readFileSync(resolve(agRoot, 'package.json'), 'utf8')).version;
const ag = require(require.resolve('ag-grid-community', { paths: [repoRoot] }));

// --- kebab<->camel (mirrors ag-grid-cljs.impl.convert) ----------------------

const camelToKebab = (s) => s.replace(/([a-z0-9])([A-Z])/g, '$1-$2').toLowerCase();

// Locale-independent code-unit order, so the sort (and thus the whole output)
// is identical across Node/ICU builds — keeps the tool deterministic.
const byKebab = (a, b) => (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0);

// --- ts-morph: property type + JSDoc metadata -------------------------------

const project = new Project({
  compilerOptions: { skipLibCheck: true },
  skipAddingFilesFromTsConfig: true,
});
project.addSourceFilesAtPaths(`${agRoot}/dist/types/src/**/*.d.ts`);

const sourceFile = (suffix) => {
  const sf = project.getSourceFiles().find((f) => f.getFilePath().endsWith(suffix));
  if (!sf) throw new Error(`source file not found: ${suffix}`);
  return sf;
};

// Coarse type classification. Only :initial? is load-bearing (feeds the
// options differ, ADR 0008); :type drives an optional second-tier dev warning,
// so a rough bucket is enough.
function classifyType(text) {
  const t = text.trim();
  if (t.includes('=>')) return 'function';
  if (/\[\]$/.test(t) || /^Array</.test(t) || /^readonly /.test(t)) return 'array';
  if (t === 'boolean') return 'boolean';
  if (t === 'number') return 'number';
  if (t === 'string') return 'string';
  if (t === 'any') return 'any';
  return 'object';
}

function firstSentence(desc) {
  const line = desc.split('\n').map((l) => l.trim()).filter(Boolean)[0] || '';
  return line.replace(/\s+/g, ' ').trim();
}

// Build a { propName -> {type, default, initial, deprecated, doc} } map for one
// interface, flattening inheritance via the type checker.
function interfaceMeta(fileSuffix, interfaceName) {
  const iface = sourceFile(fileSuffix).getInterfaceOrThrow(interfaceName);
  const out = {};
  for (const sym of iface.getType().getProperties()) {
    const name = sym.getName();
    const decl = sym.getDeclarations()[0];
    if (!decl) continue;
    const meta = { type: classifyType(sym.getTypeAtLocation(decl).getText()) };
    const jsDocs = decl.getJsDocs ? decl.getJsDocs() : [];
    for (const jd of jsDocs) {
      const desc = firstSentence(jd.getDescription());
      if (desc && !meta.doc) meta.doc = desc;
      for (const tag of jd.getTags()) {
        const tagName = tag.getTagName();
        const comment = (tag.getCommentText() || '').replace(/\s+/g, ' ').trim();
        if (tagName === 'default') meta.default = comment;
        else if (tagName === 'initial') meta.initial = true;
        else if (tagName === 'deprecated') meta.deprecated = comment || 'deprecated';
      }
    }
    out[name] = meta;
  }
  return out;
}

const gridOptionsMeta = interfaceMeta('entities/gridOptions.d.ts', 'GridOptions');
const colDefMeta = interfaceMeta('entities/colDef.d.ts', 'ColDef');
const colGroupDefMeta = interfaceMeta('entities/colDef.d.ts', 'ColGroupDef');

// --- assemble the registry blocks -------------------------------------------

// Turn a { camelName -> meta } map into a sorted [ [kebab, entry] ] list with
// the rich per-key shape {:camel :type :default :initial? :deprecated :doc}.
function keyBlock(metaByCamel, camelNames) {
  return camelNames
    .map((camel) => {
      const m = metaByCamel[camel] || { type: 'object' };
      const entry = { camel, type: m.type };
      if (m.default !== undefined && m.default !== '') entry.default = m.default;
      if (m.initial) entry.initial = true;
      if (m.deprecated) entry.deprecated = m.deprecated;
      if (m.doc) entry.doc = m.doc;
      return [camelToKebab(camel), entry];
    })
    .sort(byKebab);
}

// GridOptions keys from the runtime constant (handlers live in :events).
const gridOptionKeys = ag._GET_ALL_GRID_OPTIONS();
const gridOptions = keyBlock(gridOptionsMeta, gridOptionKeys);
const colDef = keyBlock(colDefMeta, Object.keys(colDefMeta));
const colGroupDef = keyBlock(colGroupDefMeta, Object.keys(colGroupDefMeta));

// Events: event-name -> onHandler, from the runtime handler map.
const events = Object.entries(ag._PUBLIC_EVENT_HANDLERS_MAP)
  .map(([event, handler]) => [camelToKebab(event), { event, handler }])
  .sort(byKebab);

// --- EDN serialization ------------------------------------------------------

const ednString = (s) => `"${s.replace(/\\/g, '\\\\').replace(/"/g, '\\"')}"`;

// One per-key entry as an inline EDN map, fields in a fixed order.
function ednEntry(entry) {
  const parts = [`:camel ${ednString(entry.camel)}`, `:type :${entry.type}`];
  if (entry.default !== undefined) parts.push(`:default ${ednString(entry.default)}`);
  if (entry.initial) parts.push(`:initial? true`);
  if (entry.deprecated) parts.push(`:deprecated ${ednString(entry.deprecated)}`);
  if (entry.doc) parts.push(`:doc ${ednString(entry.doc)}`);
  return `{${parts.join(' ')}}`;
}

function ednEventEntry(entry) {
  return `{:event ${ednString(entry.event)} :handler ${ednString(entry.handler)}}`;
}

// Render sorted pairs as a pretty EDN map. `indent` is the column the template
// places the opening `{` at: the first line follows `{` directly (the template
// supplies its leading whitespace), every later line is padded to align under
// it. Keep `indent` in step with the template's indentation below.
function ednBlock(pairs, render, indent) {
  const pad = ' '.repeat(indent);
  const lines = pairs.map(([k, v], i) => `${i === 0 ? '' : pad}:${k} ${render(v)}`);
  return `{${lines.join('\n')}}`;
}

const registryCljs = `(ns ag-grid-cljs.impl.registry
  "GENERATED — do not edit by hand. Regenerate with tools/codegen on an AG Grid
  dependency bump (see tools/codegen/extract.mjs, ADR 0007).

  The dev-only catalog of AG Grid option/event keys powering typo warnings and
  the kebab<->camel reference table. The runtime conversion (ADR 0005) never
  consults it — camelization is a deterministic algorithm. Only :initial? is
  load-bearing beyond dev warnings: it is the options differ's sole
  updatable-vs-initial-only classifier (ADR 0008).

  The registry value is guarded by ^boolean goog.DEBUG so :advanced compilation
  with {goog.DEBUG false} dead-code-eliminates the whole literal from production
  builds. Reference it ONLY from goog.DEBUG-guarded code to keep that true.")

(def registry
  "Rich key registry, pinned to a single AG Grid version. Blocks:
  :grid-options / :col-def / :col-group-def / :events, plus :ag-grid-version.
  Per-key shape {:camel :type :default :initial? :deprecated :doc}; :default,
  :initial?, :deprecated and :doc are present only when AG Grid declares them.
  nil in production builds (goog.DEBUG false → DCE)."
  (when ^boolean goog.DEBUG
    {:ag-grid-version ${ednString(agVersion)}
     :grid-options
     ${ednBlock(gridOptions, ednEntry, 5)}
     :col-def
     ${ednBlock(colDef, ednEntry, 5)}
     :col-group-def
     ${ednBlock(colGroupDef, ednEntry, 5)}
     :events
     ${ednBlock(events, ednEventEntry, 5)}}))
`;

writeFileSync(resolve(repoRoot, 'src/main/ag_grid_cljs/impl/registry.cljs'), registryCljs);

// --- Markdown reference (ADR 0007 §6) ---------------------------------------

const mdEscape = (s) => (s || '').replace(/\|/g, '\\|').replace(/\n/g, ' ');

function keyTable(pairs) {
  const rows = pairs.map(([kebab, e]) => {
    const init = e.initial ? 'yes' : '';
    const dep = e.deprecated ? mdEscape(e.deprecated) : '';
    return `| \`${kebab}\` | \`${e.camel}\` | ${e.type} | ${init} | ${dep} | ${mdEscape(e.doc)} |`;
  });
  return [
    '| kebab | camelCase | type | init-only? | deprecated → replacement | description |',
    '| --- | --- | --- | --- | --- | --- |',
    ...rows,
  ].join('\n');
}

function eventTable(pairs) {
  const rows = pairs.map(([kebab, e]) => `| \`${kebab}\` | \`${e.event}\` | \`${e.handler}\` |`);
  return [
    '| kebab | event name | handler prop |',
    '| --- | --- | --- |',
    ...rows,
  ].join('\n');
}

const markdown = `# AG Grid options reference

<!--
  GENERATED — do not edit by hand.
  AG Grid version: ${agVersion}
  Regenerate with \`cd tools/codegen && npm run generate\` on an AG Grid
  dependency bump (tools/codegen/extract.mjs, ADR 0007). Never hand-edit.
-->

Kebab-case option and event names for AG Grid **${agVersion}**, with the
camelCase name each converts to. Keys are ordinary EDN — the full surface is
reachable by \`assoc\`. For semantics of each option see the AG Grid docs:
<https://www.ag-grid.com/javascript-data-grid/grid-options/>.

## Grid Options

${keyTable(gridOptions)}

## Column Definitions

${keyTable(colDef)}

## Column Groups

${keyTable(colGroupDef)}

## Events

${eventTable(events)}
`;

writeFileSync(resolve(repoRoot, 'docs/reference/ag-grid-options.md'), markdown);

console.log(
  `Wrote registry (${gridOptions.length} grid-options, ${colDef.length} col-def, ` +
    `${colGroupDef.length} col-group-def, ${events.length} events) and reference ` +
    `for ag-grid-community ${agVersion}.`,
);
