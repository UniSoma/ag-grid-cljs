# tools/

Build-time tooling. Nothing here is part of the CLJS build or CI — the CLJS
build stays pure-Clojure (ADR 0007 §2, ADR 0015). These scripts run by hand.

## codegen — key registry (ADR 0007)

Extracts the AG Grid option/event surface from the repo's installed
`ag-grid-community` pin and regenerates two committed artifacts:

- `src/main/ag_grid_cljs/impl/registry.cljs` — the goog.DEBUG-guarded CLJS
  literal (dev-warnings + docs authority; DCE'd from production builds).
- `docs/reference/ag-grid-options.md` — the human kebab↔camel reference table.

Run **only when bumping the `ag-grid-community` dependency**, then commit the
regenerated output:

```sh
cd tools/codegen
npm install       # ts-morph (embeds typescript@5.x); not committed
npm run generate
```

Output is sorted, so the tool is deterministic and idempotent for a given pin.
Never hand-edit the two generated files.
