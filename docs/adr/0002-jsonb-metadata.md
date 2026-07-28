# ADR-0002 — Use JSONB for `instruments.metadata`

- Status: Accepted
- Date: 2026-07-28
- Deciders: ReconX team

## Context

`instruments` covers four asset classes (equity, FX, bond, derivative), each
with a different set of reference attributes an analyst may want to filter
or display on — sector and exchange for equities, tenor and issuer for
bonds, underlying and strike for derivatives. Modelling every attribute as
its own nullable column produces a wide, mostly-empty table and a schema
migration every time a new asset class or attribute shows up. With ~50k
trades/day referencing these instruments, the metadata is read far more
often than written.

## Decision

Add a single `metadata JSONB` column to `instruments` instead of per-attribute
columns or a separate EAV (entity-attribute-value) table. Attribute keys are
asset-class-specific by convention (e.g. `{"sector": "Banking"}` for
equities) and validated at the application layer, not the database layer.

## Consequences

**Positive**
- New asset-class attributes ship without a Liquibase migration or downtime.
- One JSONB containment query (`metadata @> '{"sector":"Banking"}'`) replaces
  what an EAV model would need as a multi-join query.
- Postgres stores JSONB in a decomposed binary form — cheaper to query than
  the `JSON` type, and supports indexing (ADR-0003).

**Negative**
- No foreign-key or NOT NULL enforcement on individual metadata fields —
  a bad key name or typo is a silent no-op filter, not a constraint violation.
- Loses the self-documenting nature of named columns; readers need
  `db/erd.md` or the application DTOs to know which keys exist per asset class.
- Reporting tools that expect flat relational columns need a view or
  `jsonb_to_record` projection instead of a plain `SELECT`.

---

**Prompt used:**

```
Decision to record: Use a JSONB metadata column on instruments instead of
per-asset-class columns or an EAV table.
Alternatives we considered: wide table with nullable per-attribute columns,
EAV (entity_attributes) table, one subtype table per asset class.
Constraints / forces: 4 asset classes with non-overlapping attribute sets,
attributes are read-heavy not write-heavy, need to add new asset classes
without a migration, still want indexable/queryable attributes.
```
