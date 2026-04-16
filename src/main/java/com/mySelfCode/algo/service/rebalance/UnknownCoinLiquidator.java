package com.mySelfCode.algo.service.rebalance;

import com.mySelfCode.algo.api.bybit.*;
import com.mySelfCode.algo.api.kucoin.*;
import com.mySelfCode.algo.dto.TradingPair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnknownCoinLiquidator {

    private final BybitBalance bybitBalance;
    private final KucoinBalance kucoinBalance;
    private final BybitCheckPrice bybitCheckPrice;
    private final KucoinCheckPrice kucoinCheckPrice;
    private final BybitInstrumentInfo bybitInstrumentInfo;
    private final KucoinInstrumentInfo kucoinInstrumentInfo;
    private final BybitSellCrypto bybitSellCrypto;
    private final KucoinSellCrypto kucoinSellCrypto;
    private final BybitTradeChecker bybitTradeChecker;
    private final KucoinTradeChecker kucoinTradeChecker;

    private static final double MIN_VALUE_TO_SELL = 2.0;

    public void liquidateUnknownCoins(java.util.List<TradingPair> pairs) {
        Set<String> knownCoins = pairs.stream()
                .map(TradingPair::getCoinSymbol)
                .collect(Collectors.toSet());

        liquidateBybitUnknownCoins(knownCoins);
        liquidateKucoinUnknownCoins(knownCoins);
    }

    private void liquidateBybitUnknownCoins(Set<String> knownCoins) {
        try {
            Map<String, Double> coins = bybitBalance.getAllCoins();
            for (Map.Entry<String, Double> entry : coins.entrySet()) {
                String coin = entry.getKey();
                if ("USDT".equals(coin) || "USDC".equals(coin) || knownCoins.contains(coin)) continue;

                try {
                    double[] prices = bybitCheckPrice.bybitCheckPrice(coin + "USDT");
                    double valueUsdt = entry.getValue() * ((prices[0] + prices[1]) / 2);
                    if (valueUsdt < MIN_VALUE_TO_SELL) {
                        log.info("[bybit] dust skip: {} = {}$", coin, valueUsdt);
                        continue;
                    }

                    log.info("[bybit] selling unknown coin {} = {}$", coin, valueUsdt);
                    double[] info = bybitInstrumentInfo.getMinTradeInfo(coin + "USDT");
                    int cryptoPrecision = toPrecision(info[2]);

                    TradingPair tempPair = new TradingPair(coin + "USDT", 0);
                    tempPair.setBybitCrypto(valueUsdt);
                    bybitSellCrypto.sellMarket(tempPair, valueUsdt, cryptoPrecision);

                    String orderId = tempPair.getBybitSellOrderId();
                    if (orderId != null) waitForBybit(orderId);

                } catch (Exception e) {
                    log.warn("[bybit] could not sell {}: {}", coin, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[bybit] error getting coins: {}", e.getMessage());
        }
    }

    private void liquidateKucoinUnknownCoins(Set<String> knownCoins) {
        try {
            Map<String, Double> coins = kucoinBalance.getAllCoins();
            for (Map.Entry<String, Double> entry : coins.entrySet()) {
                String coin = entry.getKey();
                if ("USDT".equals(coin) || "USDC".equals(coin) || knownCoins.contains(coin)) continue;

                try {
                    double[] prices = kucoinCheckPrice.kucoinCheckPrice(coin + "-USDT");
                    double valueUsdt = entry.getValue() * ((prices[0] + prices[1]) / 2);
                    if (valueUsdt < MIN_VALUE_TO_SELL) {
                        log.info("[kucoin] dust skip: {} = {}$", coin, valueUsdt);
                        continue;
                    }

                    log.info("[kucoin] selling unknown coin {} = {}$", coin, valueUsdt);
                    double[] info = kucoinInstrumentInfo.getMinTradeInfo(coin + "USDT");
                    int cryptoPrecision = toPrecision(info[2]);

                    TradingPair tempPair = new TradingPair(coin + "USDT", 0);
                    tempPair.setKucoinCrypto(valueUsdt);
                    kucoinSellCrypto.sellMarket(tempPair, valueUsdt, cryptoPrecision);

                    String orderId = tempPair.getKucoinSellOrderId();
                    if (orderId != null) waitForKucoin(orderId);

                } catch (Exception e) {
                    log.warn("[kucoin] could not sell {}: {}", coin, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[kucoin] error getting coins: {}", e.getMessage());
        }
    }

    private int toPrecision(double increment) {
        if (increment <= 0 || increment >= 1) return 0;
        return (int) Math.round(-Math.log10(increment));
    }

    private void waitForBybit(String orderId) {
        for (int i = 0; i < 15; i++) {
            String status = bybitTradeChecker.checkTrade(orderId);
            if ("Done".equals(status)) return;
            if ("Error".equals(status)) {
                log.error("[bybit] error selling unknown coin orderId={}", orderId);
                return;
            }
            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }
        log.warn("[bybit] timeout waiting for orderId={}", orderId);
    }

    private void waitForKucoin(String orderId) {
        for (int i = 0; i < 15; i++) {
            String status = kucoinTradeChecker.checkTrade(orderId);
            if ("Done".equals(status)) return;
            if ("Error".equals(status)) {
                log.error("[kucoin] error selling unknown coin orderId={}", orderId);
                return;
            }
            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }
        log.warn("[kucoin] timeout waiting for orderId={}", orderId);
    }
}