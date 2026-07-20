# Issue tracker: knot

Issues and specs for this repo are tracked with **knot**, a file-based CLI ticket
tracker. Tickets live as markdown files under `.tickets/` (config in `.knot.edn`,
project prefix `agd`, "AG Grid ClojureScript"). Closed tickets auto-move to
`.tickets/archive/`.

## The one rule: use the CLI

Read tickets only via `knot show` / `knot list` / `knot ready` / `knot blocked` /
`knot closed` / `knot prime`. Write tickets only via `knot create` / `knot start` /
`knot status` / `knot close` / `knot reopen` / `knot add-note` / `knot update` /
`knot dep` / `knot link`. Never `cat`, `grep`, `ls`, or hand-edit files under
`.tickets/` — knot resolves partial ids across live + archive and keeps frontmatter
consistent. For full guidance, invoke the `knot` skill.

## When a skill says "publish to the issue tracker"

Create a ticket:

    knot create "<title>" -t <bug|feature|task|epic|chore> -p <0-4> \
      --mode <afk|hitl> -d "<description>" [--acceptance "..."]

Pass `--json` to read back the created ticket (id under `.data.id`) without a
follow-up `knot show`.

## When a skill says "fetch the relevant ticket"

    knot show <id>          # resolves partial ids across live + archive

## When a skill says "list / find tickets"

    knot list                       # full backlog
    knot list --type bug            # filter by type (also --tag, --mode, --status,
                                    #   --assignee, --priority, --parent)
    knot ready                      # unblocked, ready to pick up
    knot ready --mode afk           # agent-runnable, unblocked

Prefer filters over eyeballing; use `--json | jq` for any decision logic.

## Closing

    knot close <id> --summary "<what shipped>"

Always pass `--summary` — it becomes the "what did we ship?" artifact.

## PRs as a request surface

Off. This is a local repo with no remote; there is no external PR queue.
