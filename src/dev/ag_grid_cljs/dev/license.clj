(ns ag-grid-cljs.dev.license
  "Dev-only compile-time license injection. The key is read from the
  AG_GRID_LICENSE environment variable at CLJS compile time and inlined into
  the (gitignored) build output — it never touches committed source. On CI,
  where the env var is unset, this expands to the empty string, which is the
  no-license path AG Grid Enterprise still runs under (console error only).")

(defmacro license-key
  "Expands to the AG_GRID_LICENSE env var at compile time, or \"\" if unset."
  []
  (or (System/getenv "AG_GRID_LICENSE") ""))
