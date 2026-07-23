# 0016. Clojars release engineering for v0.1

- Status: accepted, 2026-07-23
- Origin: grill-with-docs session, 2026-07-23 (design record; implementation tracked in a follow-up knot ticket)

v0.1 ships as a source-only jar to Clojars under the existing verified group `io.github.unisoma`, built with `tools.build` + `slipset/deps-deploy` and deployed manually (`bb deploy`) — CI stays test-only. Versioning is plain SemVer starting at the mutable `0.1.0-SNAPSHOT`, **decoupled** from AG Grid's version (the v34+ floor stays prose). The pom declares zero Maven dependencies and MIT only; the vendored EPL-1.0 cljs-bean slice travels as a bundled `THIRD-PARTY.md` notice. The tooling mirrors the sibling `io.github.unisoma/mantine-ui-wrapper`, with two conscious divergences driven by our peer-dependency model.

## Context

The spec's scope boundary parked this deliberately: *"Shipping v0.1 — Clojars release engineering, versioning, CI publish"* was consciously out of the design effort, to be redrawn fresh. This ADR is that redraw.

Starting state: coordinates and license were already locked (ADR 0006 — `io.github.unisoma/ag-grid-cljs`, MIT, Clojars); `deps.edn` is `:paths ["src/main"]` with `:deps {}` (zero runtime deps, cljs-bean vendored under `impl.bean`); there is no `build.clj`, no `:build` alias, no `pom.xml`, no git tags, and no publish tooling. CI (`ci.yml`) runs the node + browser suites on every push/PR (ADR 0015). cljdoc is the canonical docs site (ADR 0014) and depends on good pom SCM metadata. There is no `LICENSE` file at the repo root.

The sibling library `io.github.unisoma/mantine-ui-wrapper` had already solved the same problem and captured it in its own ADR. Its stack was taken as the reference to mirror where the two projects agree — but the coupling models differ: mantine tracks its wrapped library *tightly* (version-anchored, `deps.cljs`-declared npm deps, a `release-check`), whereas ag-grid-cljs is *loosely* coupled to a **peer** dependency the consumer pins.

## Decision

1. **Build/deploy tooling — `tools.build` + `slipset/deps-deploy`.** A `build.clj` under a `:build` alias with `clean` / `jar` / `install` / `deploy` tasks; the pom is written programmatically by `b/write-pom` (no checked-in `pom.xml`); the jar is source-only (`:src-dirs ["src/main"]`, no AOT, no CLJS→JS). Three carry-over tricks from the sibling: a `pom-basis` built from empty deps so the published pom has **zero** Maven dependencies; `expand-empty-elements!` post-processing so cljdoc's Jsoup parser doesn't drop the `<scm>` block; and an SCM `<tag>` that is the built commit SHA for `-SNAPSHOT` and `v<version>` for releases. Rejected: Leiningen (project is deps.edn), shadow-cljs `release` (builds JS artifacts, not Maven jars), raw `mvn deploy` (needs a hand-maintained pom).

2. **Versioning — plain SemVer, decoupled from AG Grid.** This is a **conscious divergence** from the sibling's version-anchored scheme (`9.4.1.N` = Mantine version + revision). ag-grid-cljs is loosely coupled — vanilla core, AG Grid as a peer npm dep the consumer pins, a v34+ floor, an options surface reached generically via the key registry (ADR 0007). Anchoring to AG Grid's version would falsely imply per-version coupling, force a version bump on every AG Grid patch, and contradict the documented floor. The **AG Grid compatibility floor stays prose**, never encoded in our version.

3. **Start on `0.1.0-SNAPSHOT`.** Iterate on the mutable SNAPSHOT until the public API stabilizes, then cut the immutable `0.1.0`. The version is a **single hardcoded literal** in `build.clj` — no VERSION file, no git-tag/rev-count derivation, and (divergence from the sibling) **no `release-check`**, which existed there only to validate the Mantine anchor we don't have. Consequence of the SNAPSHOT SCM-tag rule: **`git push` before deploy**, so the commit SHA cljdoc checks out actually exists on the remote.

4. **Manual deploy; CI stays test-only.** Deploy is a manual local `bb deploy`; `ci.yml` is not extended with a publish leg. This leaves the spec's *"CI publish"* bullet **deliberately deferred** — automating publish while the API is on SNAPSHOT invites accidental releases. A tag-triggered (`v*`) GitHub Actions publish leg is the intended path once the first immutable release is cut. Tasks are wrapped in a `bb.edn` (`bb jar` / `bb install` / `bb deploy` / `bb cljdoc`) for human ergonomics — the one new tool introduced here — and the release steps are documented in `docs/release.md`. Rejected: npm-script wrappers (npm isn't needed for a pure-JVM build).

5. **Clojars group + credentials.** No group-verification step is needed: `io.github.unisoma` is the GitHub-verified group Clojars auto-grants for the `github.com/UniSoma` org, and mantine-ui-wrapper already ships under it — a new artifact name under an already-verified group is auto-created on first deploy. Each deployer uses their **own Clojars deploy token** (scoped, revocable) via `CLOJARS_USERNAME` / `CLOJARS_PASSWORD` env vars read by `deps-deploy`. Because deploy is manual/local, there are **no repo secrets**.

6. **Jar contents, pom metadata, licensing.** The jar ships `src/main` plus `LICENSE` and `THIRD-PARTY.md` at its root. An MIT `LICENSE` file is **added** (none existed). The pom declares **MIT only** via `:pom-data`; the vendored cljs-bean slice is EPL-1.0 and is disclosed in the bundled `THIRD-PARTY.md`, **not** as a second pom `<license>` (that would misrepresent the artifact's license), and bundling the notice satisfies EPL-1.0's requirement that it travel with the code. **No `deps.cljs`** — the second conscious divergence from the sibling: declaring `ag-grid-community` there would auto-pull it and violate the JS-by-contract peer model (ADR 0003); consumers add the AG Grid npm packages themselves. **No GPG signing** (Clojars doesn't require it).

7. **Ancillaries.** README gains a deps.edn install snippet (`{:mvn/version "0.1.0-SNAPSHOT"}`, flagged mutable/pre-stable) and Clojars / cljdoc / CI badges — **no AG-Grid-version badge**, which would re-imply the coupling decision 2 rejects. A `bb cljdoc` task force-triggers a cljdoc rebuild (`POST cljdoc.org/api/request-build2`). A **CHANGELOG is deferred** until the first official (non-SNAPSHOT) cut.

## Consequences

- The spec's "CI publish" item remains open by choice; this ADR is where that deferral is recorded, so a future reader doesn't treat missing publish automation as an oversight.
- Two divergences from the sibling (decoupled versioning, no `deps.cljs`) are load-bearing on the peer-dependency model — reversing either would re-couple us to a specific AG Grid version and should not be done casually.
- Publishing a `-SNAPSHOT` means consumers get a mutable artifact; the README says so, and cljdoc renders whatever SHA the SCM tag points at, which is why push-before-deploy is mandatory rather than advisory.

## Considered options

- **Version anchored to AG Grid (`36.0.N`, mirroring the sibling)** — rejected: implies per-version coupling we don't have and forces churn on every AG Grid patch.
- **Cutting `0.1.0` immediately instead of `-SNAPSHOT`** — rejected: the public API isn't stable yet; SNAPSHOT keeps it mutable during iteration.
- **CI-automated publish now (tag or dispatch trigger)** — rejected for v0.1: premature while on SNAPSHOT; deferred to the first immutable release.
- **A `deps.cljs` declaring `ag-grid-community` (as the sibling does for `@mantine/*`)** — rejected: violates the peer-dependency / JS-by-contract model (ADR 0003).
- **Second pom `<license>` for the EPL-1.0 cljs-bean slice** — rejected: misrepresents the artifact license; the bundled `THIRD-PARTY.md` is the correct disclosure.
- **`release-check` version-consistency tooling** — rejected: it exists in the sibling only to enforce the wrapped-library anchor, which our decoupled scheme removes.
- **npm-script or bare `clojure -T:build` invocation** — the latter works, but `bb.edn` wrappers were chosen for human ergonomics; npm wrappers rejected (no npm role in a JVM build).

## References

- ADR 0003 — row data is JS-by-contract; AG Grid as a peer dependency (grounds decisions 2 and 6)
- ADR 0005 — conversion boundary; zero runtime deps, vendored cljs-bean (§8)
- ADR 0006 — coordinates `io.github.unisoma/ag-grid-cljs`, MIT, Clojars (locked)
- ADR 0007 — generated key registry (the generic, version-independent options surface)
- ADR 0014 — cljdoc as canonical docs (pom SCM metadata dependency)
- ADR 0015 — testing/CI (the test-only `ci.yml` this ADR leaves unchanged)
- Sibling reference: `io.github.unisoma/mantine-ui-wrapper` `build.clj` / `bb.edn` / `docs/release.md`
