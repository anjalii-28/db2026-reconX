# ADR-0003 — GIN (`jsonb_path_ops`) index over btree on `instruments.metadata`

- Status: Accepted
- Date: 2026-07-28
- Deciders: ReconX team

## Context

Following ADR-0002, `instruments.metadata` is JSONB. Analysts filter
instruments by containment queries such as
`metadata @> '{"sector":"Banking"}'` (used directly in `db/queries.sql`,
TICKET-ADV009). With 50 seeded instruments today and headroom to thousands
as more asset classes are onboarded, an unindexed JSONB column forces a
sequential scan on every containment lookup. Postgres offers two index
paths for JSONB: a btree index on an expression (e.g.
`(metadata->>'sector')`), or a GIN index on the whole column.

## Decision

Create a GIN index on `metadata` using the `jsonb_path_ops` operator class:
`CREATE INDEX idx_instruments_metadata_gin ON instruments USING GIN (metadata jsonb_path_ops);`
This supports the `@>` containment operator directly, without committing to
one specific key up front the way a btree expression index would.

## Consequences

**Positive**
- `@>` containment queries on any key/value pair use the index, not just a
  single hard-coded key — new asset-class attributes stay queryable without
  a new index.
- `jsonb_path_ops` produces a smaller, faster index than the default
  `jsonb_ops` class, at the cost of only supporting `@>` (not `?`, `?|`, `?&`).
- Index size scales with document complexity, not row count alone, which is
  acceptable given `metadata` documents stay small (a handful of keys).

**Negative**
- `jsonb_path_ops` cannot accelerate key-existence queries (`?`, `?|`, `?&`)
  — those would still need a sequential scan or a second index.
- GIN index maintenance is slower on write than btree; acceptable here since
  `instruments` is reference data (dozens to thousands of rows, rarely
  updated), not a high-write table like `trades`.
- A btree expression index on one known-hot key (e.g. `sector`) would be
  cheaper for that single lookup, but does not generalize to the other
  asset-class-specific keys we expect to add.

---

**Prompt used:**

```
Decision to record: Index instruments.metadata (JSONB) with a GIN index using
the jsonb_path_ops operator class, instead of a btree expression index on one
key or the default jsonb_ops GIN class.
Alternatives we considered: btree expression index on metadata->>'sector',
default GIN with jsonb_ops, no index (sequential scan).
Constraints / forces: queries use @> containment on varying keys per asset
class, metadata is reference data (low write volume), want smallest index
that still generalizes across future asset-class attributes.
```
