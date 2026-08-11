# Editable grids

Turning edits into writes is three problems in sequence, and each one has a
failure mode that only shows up in the app: a draft row that must not reach the
real data set until it validates, a failed write whose rollback re-enters your
own handler, and a server response you have to land back in a row without
dropping the rest of it.

None of the three ships as a function. They are stateful, or they need a
converter only you know, or they are one arm of a protocol whose other arm lives
in your handler — see [ADR 0009 § Node
operations](adr/0009-builder-catalog-v1.md#node-operations) for why the line
falls there. What the library owes you is the prose, and it is below.

Everything here assumes [[ag-grid-cljs.core/with-row-id]] is set, and that you
have picked one of the two [row recipes](options-and-conversion.md#if-your-rows-are-cljs-data)
— a row spelling paired with a matching column `:field` spelling.

## What a handler receives

Edit handlers are ordinary options (`:on-cell-value-changed`), so params arrive
as a [callback bean](options-and-conversion.md#callbacks-what-your-functions-receive-and-return).
Two properties of that bean carry the whole article:

- **`(:node params)` is the real `RowNode`.** Beans cover data, not AG Grid
  objects: a `RowNode` is a class instance and is handed to you untouched
  ([ADR 0018 § 2](adr/0018-literal-key-fallback-callback-beans.md)), so
  `.setDataValue` and `.setData` work on it directly. Its row is `(.-data node)`
  — `(:data node)` is `nil`, because there is no bean there to do the lookup.
  The beaned row is `(:data params)`, one level up.
- **`(:field (:col-def params))` is the string the column was built with** —
  `"firstName"` under the camel recipe, `"first-name"` under the literal kebab
  one. That is by definition the key the row is stored at, which is why the
  snippets below take the field from the colDef rather than spelling it again.

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

## Recipe: the rollback loop

You let the user edit a cell, send the new value to a server, and put the old
value back if the write fails. The naive version writes forever.

### The loop

`cellValueChanged` fires on every edit. The rollback is itself an edit — so it
fires `cellValueChanged` again, your handler sends *another* write, that write
fails too, and you restore again. Write → fail → restore → write. The grid looks
frozen on the old value while the network fills with doomed requests.

### The fix: a source sentinel, both arms

`.setDataValue` takes an optional fourth argument, an event source string, and
it arrives on the resulting event as `(:source params)`. Tag the rollback write
with a sentinel, and guard on that sentinel at the top of the handler. **Both
arms are required — either one alone is not a partial fix, it is no fix.**

```clojure
(defn on-cell-value-changed [{:keys [node col-def value old-value source]}]
  (let [field (:field col-def)]
    ;; Arm 2: the guard. Without it, the restore below re-enters here.
    (when-not (= "restore" source)
      (save-cell! {:field field :value value}
        {:on-error
         (fn [_]
           ;; Arm 1: the sentinel write.
           (.setDataValue ^js node field old-value "restore"))}))))
```

The sentinel is **yours**. AG Grid supplies its own source strings for user
edits, paste and fill; guard on the string you wrote rather than allow-listing
AG Grid's, which is a moving target.

A `suppressing?` atom set around the restore works until it doesn't: it has to
be scoped per node once more than one row can be rolling back, and it has to
survive an async failure arriving after the flag was cleared. The source rides
the event that caused it, so neither question comes up.

### The batch case

This is the part nobody re-derives from the single-cell version, and the reason
the guard looks optional until it isn't.

A fill-handle drag or a paste fires one `cellValueChanged` per affected cell;
the [batch-flush recipe](updating-data.md#recipe-batch-flush-for-fill-and-paste)
buffers that burst and persists it as one write. When the server rejects the
batch, you restore N cells across N nodes — and that is **N separate
`cellValueChanged` events**, one per restored cell, each hitting your handler.
There is no cycle here; each restore fires exactly once. But without the guard
you have just sent N spurious writes, for values the user never typed, against a
server that already rejected them.

```clojure
;; Buffer the burst, persist it as one write.
(defn flush-batch! [batch]                    ; [{:keys [node field value old-value]} ...]
  (save-cells! (mapv #(select-keys % [:field :value]) batch)
    {:on-error
     (fn [_]
       (doseq [{:keys [node field old-value]} batch]
         (.setDataValue ^js node field old-value "restore"))
       (show-one-error!))}))                  ; one toast, not N
```

The same guard in the same handler covers both paths, so nothing extra is
needed — but the guard is not optional just because the batch case has no cycle
in it. The event count, the sentinel arriving as `:source`, and the raw-RowNode
reads above are pinned by
`ag-grid-cljs.browser.rollback-events-test` in the committed browser suite
(ADR 0015).

## Recipe: writing a server row back into a grid row

Your write succeeds and the server hands back the authoritative row — generated
columns recomputed, a database-assigned id, a changed primary key. You want it
in the grid without refetching the page.

### `.setData` replaces, it does not merge

```clojure
(.setData ^js node #js {"server-id" 42})      ; every other column is now gone
```

Merge first, into a fresh object:

```clojure
(.setData ^js node (js/Object.assign #js {} (.-data node) js-row))
```

`js/Object.assign` into a **fresh** object rather than mutating `(.-data node)`
in place: the grid needs the `setData` call to know the row changed, and
mutating the old object first just makes the before and after identical.

Bean update operations are not a second route here. `(assoc (:data params) :k
v)` snapshots into a persistent map — a callback bean is a read view, never a
write channel.

### Write with the converter the rows were built with

This is the part that bites silently, and the reason no `merge-row-data!` ships
(ADR 0009 § Node operations).

Your rows entered the grid through some conversion, and that conversion fixed
the row's **spelling**; the column `:field`s were paired to it. A later write
into the same row has to use the same conversion, or it does not overwrite the
key you meant — it lands a **second, differently spelled key beside the live
one**. Both recipes fail this way, in opposite directions:

```clojure
;; Camel recipe: rows entered as {"rowId" 7, "unitCost" 3}
;;   a bare clj->js merge of {:row-id 9} lands
;;   {"rowId" 7, "unitCost" 3, "row-id" 9}

;; Literal kebab recipe: rows entered as {"row-id" 7, "unit-cost" 3}
;;   a camelizing merge of {:row-id 9} lands
;;   {"row-id" 7, "unit-cost" 3, "rowId" 9}
```

Nothing throws. The row now carries two row ids, the `getRowId` getter reads the
one it was told to and the other is a ghost, and the symptom surfaces far away:
rows lose identity after an edit, selection detaches, refreshes duplicate rows.
The pairing the [row recipe](options-and-conversion.md#if-your-rows-are-cljs-data)
describes for *rendering* outlives the datasource — it governs every later write
into the row.

The practical form of the rule: **name the conversion once and call it from both
places.**

```clojure
(defn row->js [row] (clj->js row))            ; literal kebab recipe
;; (defn row->js [row] (clj->js row :keyword-fn ag/kebab->camel))   ; camel recipe

;; datasource
(defn get-rows [params] (... (mapv row->js rows) ...))

;; write-back
(defn merge-row! [^js node row]
  (.setData node (js/Object.assign #js {} (.-data node) (row->js row))))
```

If you cannot point at the single function both paths use, you do not yet know
that your write-back spells the row the way your datasource did. This is also
why the library cannot do the merge for you: a library-side merge would have to
convert, and only you know which recipe your rows are on — the same reason
`:field` is not coerced for you.

### Which channel, by row model

- **Client-side row model:** prefer [[ag-grid-cljs.core/transact!]] with
  `:update`. It is the row model's own update channel and it matches rows by id
  for you. Reach for the node-level merge above only when you already hold the
  node and want it changed in place.
- **Infinite row model:** there is no transaction API — `applyTransaction` is
  gated to the client-side model. The node-level merge is the in-place option;
  the alternative is purging the cache and refetching, which costs a round trip
  and loses the cached blocks around your scroll position.
- **Server-side row model:** `applyServerSideTransaction` exists, but it adds
  and removes rows in a store. For putting a freshly-returned row back into a
  node you already hold, the node-level merge is still the channel.

### When the row id changes

If the merged row carries a *different* id than the node was created with, you
have changed the row's identity, not just its contents. Whether the grid follows
depends on your row model and your `getRowId`; a purge-and-refetch is the honest
move when a write can reassign a primary key. Merging the new id into the node is
enough only when nothing downstream has cached the old one.
