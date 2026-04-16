package com.mySelfCode.algo.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
public class TradingPair {
    private final String symbol;
    private final double percent;

    private volatile double bybitAskPrice;
    private volatile double bybitBidPrice;
    private volatile double kucoinAskPrice;
    private volatile double kucoinBidPrice;

    private volatile double bybitCoins;
    private volatile double bybitCrypto;

    private volatile double kucoinCoins;
    private volatile double kucoinCrypto;

    private volatile String bybitBuyOrderId;
    private volatile String bybitSellOrderId;
    private volatile String kucoinBuyOrderId;
    private volatile String kucoinSellOrderId;

    private volatile String process = "NON";
    private final AtomicBoolean checkInProgress = new AtomicBoolean(false);
    private volatile boolean buyOnBybit = false;
    private final AtomicInteger wait = new AtomicInteger(0);
    private final AtomicInteger waitAlgoLog = new AtomicInteger(0);
    private final AtomicInteger waitErrorLog = new AtomicInteger(0);

    public TradingPair(String symbol, double percent) {
        this.symbol = symbol;
        this.percent = percent;
    }

    public String getCoinSymbol() {
        return symbol.replaceAll(" ", "").replace("USDT", "");
    }

    public String getBybitSymbol() {
        return symbol.replaceAll(" ", "");
    }

    public String getKucoinSymbol() {
        return symbol.replaceAll(" ", "-");
    }
}