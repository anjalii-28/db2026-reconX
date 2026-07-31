package com.dbtraining.reconx.model;

import java.time.LocalDate;
public sealed interface TradeType
        permits EquityTrade, FXTrade, BondTrade, DerivativeTrade {

    TradeRef tradeRef();
    Money notional();
    LocalDate tradeDate();
    AssetClass assetClass();

    enum AssetClass { EQUITY, FX, BOND, DERIVATIVE }
}
