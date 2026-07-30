package com.dbtraining.reconx.service;
import com.dbtraining.reconx.model.BondTrade;
import com.dbtraining.reconx.model.DerivativeTrade;
import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.FXTrade;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.TradeType;
import io.micrometer.core.annotation.Timed;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * TICKET-ADV033 — ReconciliationEngine using Streams (parallel matching)
 * TICKET-ADV037 — CompletableFuture: parallel recon by counterparty
 * TICKET-ADV047 — Edge cases: empty/single/all-mismatched inputs handled
 * TICKET-ADV084 — @Timed exports reconciliation_duration_seconds histogram
 *
 * WHAT:    Compares internal trades against external (counterparty) trades and
 *          returns a ReconResult per internal trade (MATCHED or BREAK).
 * HOW:     Index externals by tradeRef, then stream internals and look each
 *          up. CompletableFuture variant batches by counterparty for
 *          throughput on large books.
 * WHY:     This is the spine of the product. Everything else (REST API,
 *          Kafka consumers, dashboard) ultimately calls into here.
 * OBSERVE: Histogram appears at /actuator/prometheus under
 *          reconciliation_duration_seconds.
 * ============================================================================
 */
@Service
public class ReconciliationEngine {

   @Timed(value = "reconciliation.duration", description = "Wall time of reconcile()",
       percentiles = {0.5, 0.95, 0.99}, histogram = true)
public List<ReconResult> reconcile(List<TradeType> internal,
                                   List<TradeType> external,
                                   ReconciliationRule rule) {

    if (internal == null || internal.isEmpty()) {
        return List.of();
    }

    if (external == null) {
        external = List.of();
    }

    Map<String, TradeType> externalByRef = external.stream()
            .collect(Collectors.toMap(
                    t -> t.tradeRef().value(),
                    Function.identity(),
                    (a, b) -> a
            ));

    return internal.parallelStream()
            .map(in -> matchOne(
                    in,
                    externalByRef.get(in.tradeRef().value()),
                    rule
            ))
            .toList();
}
    

    /**
     * TICKET-ADV037 — split by counterparty, reconcile each batch concurrently,
     * combine into a single result list. Caller passes one external feed per
     * counterparty (typical real-world shape).
     */
 public CompletableFuture<List<ReconResult>> reconcileByCounterparty(
        Map<Long, List<TradeType>> internalByCp,
        Map<Long, List<TradeType>> externalByCp,
        ReconciliationRule rule) {

    List<CompletableFuture<List<ReconResult>>> futures =
            internalByCp.entrySet().stream()
                    .map(entry ->
                            CompletableFuture.supplyAsync(() ->
                                    reconcile(
                                            entry.getValue(),
                                            externalByCp.getOrDefault(entry.getKey(), List.of()),
                                            rule)))
                    .toList();

    return CompletableFuture
            .allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v ->
                    futures.stream()
                            .flatMap(f -> f.join().stream())
                            .toList());
}

 private ReconResult matchOne(TradeType internal,
                             TradeType external,
                             ReconciliationRule rule) {

    if (external == null) {
        return ReconResult.breakResult(
                internal.tradeRef().value(),
                "MISSING_EXTERNAL",
                "No matching external trade found"
        );
    }

    BigDecimal[] internalValues = priceQty(internal);
    BigDecimal[] externalValues = priceQty(external);

    boolean matched = rule.matches(
            internalValues[0],
            internalValues[1],
            externalValues[0],
            externalValues[1]
    );

    if (matched) {
        return ReconResult.matched(internal.tradeRef().value());
    }

    return ReconResult.breakResult(
            internal.tradeRef().value(),
            "VALUE_MISMATCH",
            "Price/Quantity outside tolerance"
    );
}

    /** TICKET-ADV018 — exhaustive switch over the sealed hierarchy. */
 private BigDecimal[] priceQty(TradeType t) {
    return switch (t) {
        case EquityTrade e ->
                new BigDecimal[]{e.price(), e.quantity()};

        case FXTrade f ->
                new BigDecimal[]{f.fxRate(), f.notionalCcy1()};

        case BondTrade b ->
                new BigDecimal[]{b.couponRate(), b.faceValue()};

        case DerivativeTrade d ->
                new BigDecimal[]{d.strike(), d.quantity()};
    };
}
}
