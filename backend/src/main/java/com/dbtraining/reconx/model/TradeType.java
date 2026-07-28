package com.dbtraining.reconx.model;

import java.time.LocalDate;
import java.util.Comparator;

/**
 * Sealed root of the platform trade hierarchy.
 *
 * <p>The explicit permits list keeps the set of supported asset classes closed,
 * allowing consumers to handle every trade type exhaustively. Its natural order
 * is newest trade date first, then trade reference and asset class ascending.</p>
 */
public sealed interface TradeType extends Comparable<TradeType>
        permits EquityTrade, FXTrade, BondTrade, DerivativeTrade {

    /**
     * Returns the stable business key for this trade.
     *
     * @return the non-null trade reference used for identity and ordering
     */
    TradeRef tradeRef();

    /**
     * Returns the monetary notional used in reconciliation calculations.
     *
     * @return the non-null notional in the trade's reporting currency
     */
    Money notional();

    /**
     * Returns the business date on which the trade was executed.
     *
     * @return the non-null trade date
     */
    LocalDate tradeDate();

    /**
     * Returns the closed asset-class discriminator for this trade.
     *
     * @return the asset class represented by this instance
     */
    AssetClass assetClass();

    /** Shared natural ordering: newest trades first, then reference ascending. */
    Comparator<TradeType> NATURAL = Comparator
            .comparing(TradeType::tradeDate)
            .reversed()
            .thenComparing(trade -> trade.tradeRef().value())
            .thenComparing(trade -> trade.assetClass().name());

    /**
     * Compares this trade using {@link #NATURAL}.
     *
     * @param other another trade to compare
     * @return a negative value when this trade sorts first, zero for the same reference,
     *         or a positive value otherwise
     * @throws NullPointerException if {@code other} is null
     */
    @Override
    default int compareTo(TradeType other) {
        return NATURAL.compare(this, other);
    }

    /** The closed set of asset classes accepted by the platform. */
    enum AssetClass { EQUITY, FX, BOND, DERIVATIVE }
}
