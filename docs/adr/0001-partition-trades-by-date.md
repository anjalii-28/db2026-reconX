# ADR-0001 — Partition the `trades` table by `trade_date`

- Status: Accepted
- Date: 2026-07-28
- Deciders: ReconX team

## Context

`trades` is our highest-volume table — ~50k inserts/day, 5-year retention =
~91M rows at steady state. The vast majority of queries (dashboards,
recon runs, analyst lookups) filter by a date range (often single day or
single month). A single unpartitioned table forces full-table scans for
date-range deletes and complicates archival of older trade data for the
5-year-retention SLA.

## Decision

Partition `trades` by RANGE on `trade_date`, with one partition per calendar
month. The primary key includes `trade_date` to satisfy Postgres' partitioning
constraint. Child partitions are named `trades_YYYY_MM` and the current
rolling 12 months are pre-created by the Liquibase changeset on boot.

## Consequences

**Positive**
- Partition pruning eliminates 11/12 of the data on a typical month-filtered query.
- Archival becomes a DDL operation (`DETACH PARTITION`), not a row-level delete.
- Indexes are smaller per partition, faster to maintain.

**Negative**
- Composite PK `(id, trade_date)` complicates JPA `@Id` mapping.
- Cross-partition unique constraints (e.g., `trade_ref`) require `UNIQUE (trade_ref, trade_date)` instead of a single-column unique.
- FKs into `trades(id)` from `settlements`/`recon_breaks` cannot reference a non-unique column on the partitioned parent — those FKs had to be dropped or deferred (see 004/006 changelogs).
- Pre-creating partitions is a recurring ops task — must stay automated (the `DO $$` block regenerates the rolling window on every boot).

---

**Prompt used:**

```
Decision to record: Partition `trades` by RANGE on trade_date, one partition
per calendar month, rolling 12-month window.
Alternatives we considered: no partitioning (single table), hash partitioning
on trade_ref, partitioning by counterparty_id.
Constraints / forces: 91M-row steady state at 5yr retention, queries are
almost always date-bounded, need cheap archival of old months, Postgres
requires partition key in every unique constraint on the parent.
```
