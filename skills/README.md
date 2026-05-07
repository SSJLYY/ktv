# KTV Project Skills

This `skills/` directory now serves two purposes:

- keep historical project notes
- provide a triggerable repo-local Codex skill

## Primary Skill

- `ktv-project/`
  - main skill for this repository
  - use it for repo-wide reading, codereview, bug fixing, API tracing, playback and queue debugging, and doc updates

Entry point:

- [`skills/ktv-project/SKILL.md`](./ktv-project/SKILL.md)

On-demand references:

- [`skills/ktv-project/references/repo-map.md`](./ktv-project/references/repo-map.md)
- [`skills/ktv-project/references/workflows.md`](./ktv-project/references/workflows.md)
- [`skills/ktv-project/references/contracts.md`](./ktv-project/references/contracts.md)
- [`skills/ktv-project/references/commands.md`](./ktv-project/references/commands.md)
- [`skills/ktv-project/references/review-checklist.md`](./ktv-project/references/review-checklist.md)

## Notes

- Prefer `ktv-project` when the task is about reading the project, fixing bugs, tracing contracts, or continuing codereview passes.
- Legacy `skills/*.md` files remain as historical references only. Some contain mojibake or stale information and should not be treated as the source of truth.
- Actual top-level modules in this repo:
  - `ktv-backend`
  - `admin-frontend`
  - `room-frontend`
  - `sql`
  - `docs`
