# Updating data

AG Grid is an imperative widget: once created, you never re-render it — you
drive it through its API. This library exposes that as **three explicit
channels**, and choosing the right one is the whole skill (ADR 0004).

| Channel | Function | For |
| --- | --- | --- |
| Full row swap | [[ag-grid-cljs.core/set-rows!]] | "here is the complete row set now" |
| Row transaction | [[ag-grid-cljs.core/transact!]] | fine-grained `:add` / `:update` / `:remove` |
| Options PATCH | [[ag-grid-cljs.core/update-grid!]] | changing grid *options* (features, columns, callbacks) |

Row data never travels through the options channel — that is what preserves
scroll, selection, and focus across an update. Everything below assumes you set
[[ag-grid-cljs.core/with-row-id]] at creation: without a stable row id AG Grid
cannot diff, and every update degrades to a full teardown.

## The row channels: `set-rows!` and `transact!`

[[ag-grid-cljs.core/set-rows!]] hands AG Grid the **complete** row array. With a
row id set, AG Grid diffs the new array against what it holds and touches only
what changed — added, removed, and updated rows are reconciled, and grid state
survives the swap. Rows are [JS by contract](options-and-conversion.md#row-data-is-js-by-contract):
a JS array of JS objects.

```clojure
(ag/set-rows! handle (into-array (map person->row people)))
```

[[ag-grid-cljs.core/transact!]] applies a **fine-grained** transaction when you
already know the delta — no full array, no diff:

```clojure
(ag/transact! handle {:add    [(person->row p)]
                      :update [(person->row q)]
                      :remove [(person->row r)]
                      :add-index 0})
```

The tx map is forward-converted like any options value; `:update` and `:remove`
match existing rows by their row id. AG Grid's `RowNodeTransaction` result comes
back untouched, so you can read what actually changed.

Rule of thumb: `set-rows!` when your state model already holds the whole
collection (the diff is cheap and you avoid dual bookkeeping — see below);
`transact!` when a single mutation gives you the exact delta and re-deriving the
whole array would be wasteful.

## The options channel: `update-grid!`

[[ag-grid-cljs.core/update-grid!]] is a **PATCH**, not full-state (ADR 0008):
you pass only the keys to change, absent keys are left exactly as they are, and
present keys are compared by `=` against the last-applied map so only genuinely
changed keys emit a `setGridOption`.

```clojure
(-> handle
    (ag/update-grid! {:pagination true})
    (ag/update-grid! {:quick-filter-text "ada"}))
```

It returns the handle with its stash merged forward, so **thread the returned
handle** — the next diff compares against the true applied state. Two keys get
special treatment:

- `:row-data` is refused with a dev warning — it belongs to the row channels.
- Registry-classified *initial-only* options (things that cannot change after
  creation) are ignored with a dev warning rather than silently forcing a
  destructive recreate.

Callbacks are ordinary options, so re-supplying `:on-cell-clicked` with a new
closure updates the handler through this same channel — the latest closure wins.

## Pattern: `set-rows!` from your db

The default consumer pattern, proven against the Fulcro reference bar (ADR
0004). Keep **one** source of truth — your own state — and derive rows from it;
never mirror each mutation into the grid by hand.

```clojure
;; state is the single source of truth; a row is a pure projection of an entity.
(defn person->row [{:keys [id name salary]}]
  #js {:id (str id) :name name :salary salary})

(defn sync-grid! [handle state]
  (ag/set-rows! handle (into-array (map person->row (:people state)))))

;; every mutation just updates state, then re-syncs — one bookkeeping path.
(swap! state update-salary id +1000)
(sync-grid! handle @state)
```

Because AG Grid diffs by row id, this full swap is state-preserving: scroll,
selection, and focus survive. Reach for `transact!` only when a hot path makes
re-deriving the whole array measurably too expensive.

## Recipe: optimistic pending rows

A new-row form that shows the row *immediately*, lets the user fill it in, and
only commits it to the real data set once it validates. The mechanism is AG
Grid's **pinned top rows** (`:pinned-top-row-data`) plus an edit router keyed by
a temporary row id. It is stateful, so it is a recipe, not a builder (ADR 0009);
the state below is a plain atom, framework-agnostic — swap it for your
framework's state cell unchanged.

```clojure
(def pending (atom nil))   ; the one draft row, or nil

(defn new-draft [] #js {:id (str "tmp-" (random-uuid)) :name "" :salary nil})

(defn temp? [row] (.startsWith (.-id row) "tmp-"))

(defn start-draft! [handle]
  (reset! pending (new-draft))
  (ag/update-grid! handle {:pinned-top-row-data #js [@pending]}))

;; route every edit: temp rows stay in the pinned buffer; once complete, they
;; graduate into the real row set via a transaction and the pin clears.
(defn on-cell-value-changed [handle e]
  (let [row (:data e)]
    (when (temp? row)
      (if (complete? row)
        (do (ag/transact! handle {:add [row] :add-index 0})
            (reset! pending nil)
            (ag/update-grid! handle {:pinned-top-row-data #js []}))
        (reset! pending row)))))   ; still a draft — keep buffering edits
```

Wire the router as `:on-cell-value-changed` in your options. The persisted rows
flow through the row channel (`transact!`); the draft never pollutes the real
data set until it is ready.

## Recipe: batch-flush for fill and paste

A single fill-handle drag or clipboard paste fires **one
`cellValueChanged` per affected cell** — persisting inside that handler means
dozens of writes for one gesture. The recipe brackets the gesture with AG Grid's
`fillStart`/`fillEnd` and `pasteStart`/`pasteEnd` events: buffer the individual
changes while a gesture is in flight, then flush once when it ends. Again
stateful, again framework-agnostic (a plain atom).

```clojure
(def batch (atom nil))   ; a vector while a gesture runs, else nil

(defn begin! [_e] (reset! batch []))

(defn on-cell-value-changed [handle e]
  (if @batch
    (swap! batch conj (:data e))          ; gesture in flight: buffer, don't persist
    (persist! [(:data e)])))              ; lone edit: persist immediately

(defn flush! [handle _e]
  (when-let [rows (seq @batch)]
    (persist! rows)                        ; one write for the whole gesture
    (ag/transact! handle {:update (into-array rows)}))
  (reset! batch nil))
```

```clojure
(-> (ag/options)
    (assoc :on-fill-start  begin!
           :on-paste-start begin!
           :on-cell-value-changed (partial on-cell-value-changed handle)
           :on-fill-end    (partial flush! handle)
           :on-paste-end   (partial flush! handle)))
```

A lone edit (no surrounding gesture) persists straight away; a fill or paste
collapses to a single `persist!` and a single `transact!`. The buffer is just a
vector — nothing here is React- or Reagent-specific.
