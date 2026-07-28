# ADR prompt template

Every ADR in this folder was drafted by instantiating the prompt below and
critically reviewing the output before committing it. Reuse this template
for future decisions.

```
You are an enterprise software architect. Write an Architecture Decision Record
(ADR) in the Michael Nygard format (Title, Status, Context, Decision,
Consequences) for the following decision.

System: ReconX, a near-prod trade reconciliation platform.
Stack: PostgreSQL 16, Spring Boot 3, Kafka, React.
Scale: ~50,000 trades/day, 5-year retention, 10 concurrent recon analysts.

Decision to record: <ONE LINE DESCRIBING THE DECISION>

Alternatives we considered: <LIST 2-3>

Constraints / forces: <LIST 2-3>

Format: Markdown, Nygard 5-section template, no fluff. Keep under 300 words.
Include a "Status: Accepted | Date: <YYYY-MM-DD>" line.
```

## ADRs in this folder

| # | Decision |
|---|---|
| [0001](0001-partition-trades-by-date.md) | Partition `trades` by `trade_date` |
| [0002](0002-jsonb-metadata.md) | JSONB for `instruments.metadata` |
| [0003](0003-gin-over-btree.md) | GIN (`jsonb_path_ops`) index over btree on `metadata` |
