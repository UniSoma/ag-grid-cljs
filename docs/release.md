# Releasing ag-grid-cljs

v0.1 ships as a **source-only jar** to [Clojars](https://clojars.org/io.github.unisoma/ag-grid-cljs)
under the GitHub-verified group `io.github.unisoma`. Deploy is a **manual, local**
step — CI stays test-only (ADR 0016). This document is the runbook.

The version is a single literal in `build.clj` (`0.1.0-SNAPSHOT` today). Bump it
there when cutting a new version.

## One-time setup: a Clojars deploy token

Each deployer uses their own scoped, revocable deploy token — there are no shared
repo secrets.

1. Log in to [clojars.org](https://clojars.org) with an account that is a member
   of the `io.github.unisoma` group. (Membership is auto-granted to the
   `github.com/UniSoma` org; a new artifact name under the already-verified group
   is auto-created on first deploy, so no group-verification step is needed.)
2. Go to **Dashboard → Deploy Tokens** and create a token. Scope it to the
   `io.github.unisoma/ag-grid-cljs` artifact (or the whole group) and copy it — it
   is shown only once.

## Every deploy

`deps-deploy` reads credentials from the environment. Export your Clojars
**username** and the **token** (not your account password) as the password:

```bash
export CLOJARS_USERNAME="your-clojars-username"
export CLOJARS_PASSWORD="CLOJARS_xxxxxxxx…"   # the deploy token
```

### 1. Push before you deploy

For a `-SNAPSHOT` version the pom's SCM `<tag>` is the **built commit SHA**
(`build.clj`). cljdoc checks that SHA out to render docs, so it must exist on the
remote before you deploy. This is mandatory, not advisory:

```bash
git push
```

### 2. Deploy

```bash
bb deploy
```

`bb deploy` builds the source-only jar and pushes the artifact + pom to Clojars via
`deps-deploy` (`:installer :remote`). To sanity-check the artifact locally first,
`bb install` places it in your local `~/.m2`:

```bash
bb install   # optional: build + install to ~/.m2 with the pom
bb deploy    # build + push to Clojars
```

No GPG signing is required (Clojars doesn't require it for this group).

### 3. Trigger cljdoc

cljdoc auto-ingests new Clojars releases, so a rebuild usually happens on its own.
To force one immediately:

```bash
bb cljdoc
```

`bb cljdoc` scrapes the coordinate and version from `build.clj` and POSTs them to
`https://cljdoc.org/api/request-build2`. Docs then render at
<https://cljdoc.org/d/io.github.unisoma/ag-grid-cljs>.

## bb tasks

| Task         | What it does                                                              |
|--------------|---------------------------------------------------------------------------|
| `bb jar`     | Build the source-only jar (`target/*.jar` + pom).                         |
| `bb install` | Build and install the jar into `~/.m2` with its pom.                      |
| `bb deploy`  | Build and push the jar to Clojars (needs the env vars above).             |
| `bb cljdoc`  | Force-trigger a cljdoc rebuild for the current coordinate + version.      |

All three build tasks delegate to `clojure -T:build` (see `build.clj`).

## Notes

- **SNAPSHOT is mutable.** Consumers get a mutable artifact until the API
  stabilizes and an immutable `0.1.0` is cut. The README flags this.
- **No CI publish leg** and **no CHANGELOG** yet — both deferred to the first
  immutable release (ADR 0016). A tag-triggered (`v*`) publish workflow is the
  intended path then.
