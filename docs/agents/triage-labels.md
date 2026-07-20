# Triage roles → knot

The skills speak in five canonical triage roles. knot has no flat "label" system;
instead each role maps onto knot's native fields where one exists, falling back to
tags only for states knot doesn't model natively.

| Canonical role   | knot mechanism                              | How to apply                                          |
| ---------------- | ------------------------------------------- | ----------------------------------------------------- |
| `needs-triage`   | intake status `open` + `needs-triage` tag   | `knot update <id> --add-tag needs-triage`             |
| `needs-info`     | `needs-info` tag                            | `knot update <id> --add-tag needs-info`               |
| `ready-for-agent`| **native `--mode afk`**                     | `knot update <id> --mode afk`                         |
| `ready-for-human`| **native `--mode hitl`** (default)          | `knot update <id> --mode hitl`                        |
| `wontfix`        | **terminal close** (auto-archives) + tag    | `knot close <id> --summary "wontfix: <reason>"` then `knot update <id> --add-tag wontfix` |

## Notes for skills

- **"Which tickets can an agent grab?"** → `knot ready --mode afk`. This is knot's
  native query — don't scan for a `ready-for-agent` tag.
- **"Requires a human"** → `--mode hitl` (the default for new tickets). Don't
  autonomously pick up `hitl` tickets unless the user authorizes that ticket.
- **`needs-triage`** means the ticket is still in the `open` intake lane and its
  mode/priority haven't been decided. Remove the tag once triaged.
- **`needs-info`** is a pure tag; knot has no "waiting on reporter" state. Clear it
  with `knot update <id> --remove-tag needs-info`.
- **`wontfix`** is terminal: `knot close` archives the file. The `wontfix` tag
  distinguishes it from work that actually shipped.
