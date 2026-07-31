package com.dbtraining.reconx.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.dbtraining.reconx.exception.InvalidTradeException;

public final class TradeFactory {

    private TradeFactory() { }

    public static TradeType create(String assetClass, Map<String, Object> parameters) {
        Objects.requireNonNull(assetClass, "assetClass");
        Objects.requireNonNull(parameters, "parameters");
        TradeType.AssetClass parsedAssetClass = TradeType.AssetClass.valueOf(
                assetClass.toUpperCase(Locale.ROOT));

        try {
            return switch (parsedAssetClass) {
                case EQUITY -> equity(parameters);
                case FX -> fx(parameters);
                case BOND -> bond(parameters);
                case DERIVATIVE -> derivative(parameters);
            };
        } catch (RuntimeException exception) {
            throw new InvalidTradeException(
                    "Invalid %s trade: %s".formatted(parsedAssetClass, exception.getMessage()));
        }
    }

    private static EquityTrade equity(Map<String, Object> parameters) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of((String) required(parameters, "tradeRef")))
                .instrumentSymbol((String) required(parameters, "symbol"))
                .quantity(new BigDecimal(required(parameters, "quantity").toString()))
                .price(new BigDecimal(required(parameters, "price").toString()))
                .currency((String) required(parameters, "currency"))
                .side(Side.valueOf((String) required(parameters, "side")))
                .tradeDate(LocalDate.parse((String) required(parameters, "tradeDate")))
                .counterpartyId(((Number) required(parameters, "counterpartyId")).longValue())
                .build();
    }

    private static FXTrade fx(Map<String, Object> parameters) {
        return FXTrade.builder()
                .tradeRef(TradeRef.of((String) required(parameters, "tradeRef")))
                .ccy1((String) required(parameters, "ccy1"))
                .ccy2((String) required(parameters, "ccy2"))
                .notionalCcy1(new BigDecimal(required(parameters, "notionalCcy1").toString()))
                .fxRate(new BigDecimal(required(parameters, "fxRate").toString()))
                .side(Side.valueOf((String) required(parameters, "side")))
                .tradeDate(LocalDate.parse((String) required(parameters, "tradeDate")))
                .counterpartyId(((Number) required(parameters, "counterpartyId")).longValue())
                .build();
    }

    private static BondTrade bond(Map<String, Object> parameters) {
        return BondTrade.builder()
                .tradeRef(TradeRef.of((String) required(parameters, "tradeRef")))
                .isin((String) required(parameters, "isin"))
                .faceValue(new BigDecimal(required(parameters, "faceValue").toString()))
                .couponRate(new BigDecimal(required(parameters, "couponRate").toString()))
                .maturityDate(LocalDate.parse((String) required(parameters, "maturityDate")))
                .currency((String) required(parameters, "currency"))
                .side(Side.valueOf((String) required(parameters, "side")))
                .tradeDate(LocalDate.parse((String) required(parameters, "tradeDate")))
                .counterpartyId(((Number) required(parameters, "counterpartyId")).longValue())
                .build();
    }

    private static DerivativeTrade derivative(Map<String, Object> parameters) {
        return DerivativeTrade.builder()
                .tradeRef(TradeRef.of((String) required(parameters, "tradeRef")))
                .underlying((String) required(parameters, "underlying"))
                .strike(new BigDecimal(required(parameters, "strike").toString()))
                .quantity(new BigDecimal(required(parameters, "quantity").toString()))
                .expiry(LocalDate.parse((String) required(parameters, "expiry")))
                .optionType(DerivativeTrade.OptionType.valueOf(
                        (String) required(parameters, "optionType")))
                .currency((String) required(parameters, "currency"))
                .side(Side.valueOf((String) required(parameters, "side")))
                .tradeDate(LocalDate.parse((String) required(parameters, "tradeDate")))
                .counterpartyId(((Number) required(parameters, "counterpartyId")).longValue())
                .build();
    }

    private static Object required(Map<String, Object> parameters, String key) {
        return Objects.requireNonNull(parameters.get(key), key);
    }
}
