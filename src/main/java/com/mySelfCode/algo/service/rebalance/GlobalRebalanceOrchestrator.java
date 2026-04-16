package com.mySelfCode.algo.service.rebalance;

import com.mySelfCode.algo.api.bybit.BybitInstrumentInfo;
import com.mySelfCode.algo.api.bybit.BybitTradeChecker;
import com.mySelfCode.algo.api.kucoin.KucoinInstrumentInfo;
import com.mySelfCode.algo.api.kucoin.KucoinTradeChecker;
import com.mySelfCode.algo.dto.*;
import com.mySelfCode.algo.entity.HistoryResponse;
import com.mySelfCode.algo.service.market.BalanceService;
import com.mySelfCode.algo.service.market.PriceService;
import com.mySelfCode.algo.service.trading.OrderExecutor;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalRebalanceOrchestrator {

    private final TradingPairRegistry registry;
    private final RebalanceCalculator calculator;
    private final OrderExecutor orderExecutor;
    private final UnknownCoinLiquidator liquidator;
    private final PriceService priceService;
    private final BalanceService balanceService;
    private final ExchangeUsdt exchangeUsdt;
    private final BybitInstrumentInfo bybitInstrumentInfo;
    private final KucoinInstrumentInfo kucoinInstrumentInfo;
    private final BybitTradeChecker bybitTradeChecker;
    private final KucoinTradeChecker kucoinTradeChecker;
    private final HistoryResponse historyResponse;

    @PreDestroy
    public void shutdown() {
        rebalanceExecutor.shutdown();
    }

    private final ExecutorService rebalanceExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "rebalance-orchestrator");
        t.setPriority(Thread.MAX_PRIORITY);
        return t;
    });

    public Map<String, MinTradeInfo> buildMinTradeInfoMap(List<TradingPair> pairs) {
        Map<String, MinTradeInfo> result = new HashMap<>();
        for (TradingPair pair : pairs) {
            double[] bybitInfo = bybitInstrumentInfo.getMinTradeInfo(pair.getSymbol());
            double[] kucoinInfo = kucoinInstrumentInfo.getMinTradeInfo(pair.getSymbol());

            MinTradeInfo info = new MinTradeInfo();
            info.setMinCryptoBybit(bybitInfo[0]);
            info.setMinUsdtBybit(bybitInfo[1]);
            info.setCryptoPrecisionBybit(toPrecision(bybitInfo[2]));
            info.setUsdtPrecisionBybit(toPrecision(bybitInfo[3]));

            info.setMinCryptoKucoin(kucoinInfo[0]);
            info.setMinUsdtKucoin(kucoinInfo[1]);
            info.setCryptoPrecisionKucoin(toPrecision(kucoinInfo[2]));
            info.setUsdtPrecisionKucoin(toPrecision(kucoinInfo[3]));

            result.put(pair.getSymbol(), info);
        }
        return result;
    }

    private int toPrecision(double increment) {
        if (increment <= 0 || increment >= 1) return 0;
        return (int) Math.round(-Math.log10(increment));
    }

    public void startGlobalRebalanceIfNeeded(TradingPair triggerPair, Map<String, MinTradeInfo> infoMap) {
        if (!triggerPair.getCheckInProgress().compareAndSet(false, true)) return;

        log.info("[{}] initiating global rebalance", triggerPair.getSymbol());
        List<TradingPair> pairs = registry.getPairs();
        pairs.forEach(p -> {
            p.setProcess("REBALANCING");
            if(!p.getCheckInProgress().compareAndSet(false, true)) {
                log.warn("[{}] was already in progress during rebalance start, forcing state", p.getSymbol());
            }
            p.getCheckInProgress().set(true);
        });

        CompletableFuture.runAsync(() -> {
            try {
                boolean success = executeGlobalRebalance(pairs, infoMap);
                if (success) {
                    pairs.forEach(p -> {
                        p.setProcess("READY_FOR_ALGO");
                        p.getCheckInProgress().set(false);
                        History h = new History();
                        h.setType("R");
                        h.setSymbol(p.getSymbol());
                        h.setBybitPrice((p.getBybitAskPrice() + p.getBybitBidPrice()) / 2);
                        h.setKucoinPrice((p.getKucoinAskPrice() + p.getKucoinBidPrice()) / 2);
                        historyResponse.addToHistory(h);
                    });
                    log.info("Global rebalance completed successfully");
                } else {
                    pairs.forEach(p -> {
                        p.setProcess("ERROR");
                        p.getCheckInProgress().set(false);
                    });
                    log.error("Global rebalance failed");
                }
            } catch (Exception e) {
                log.error("Global rebalance crashed", e);
                pairs.forEach(p -> p.setProcess("ERROR"));
            }
        }, rebalanceExecutor);
    }

    private boolean executeGlobalRebalance(List<TradingPair> pairs, Map<String, MinTradeInfo> infoMap) {
        liquidator.liquidateUnknownCoins(pairs);

        priceService.updateAllPricesAsync().join();
        balanceService.updateAllBalancesAsync().join();

        double bybitTotal = exchangeUsdt.getBybitUsdt() +
                pairs.stream().mapToDouble(TradingPair::getBybitCrypto).sum();
        double kucoinTotal = exchangeUsdt.getKucoinUsdt() +
                pairs.stream().mapToDouble(TradingPair::getKucoinCrypto).sum();

        RebalanceCalculator.RebalancePlan bybitPlan = calculator.calculatePlan(
                pairs, bybitTotal, infoMap,
                TradingPair::getBybitCrypto,
                (p, info) -> info.getMinUsdtBybit()
        );
        RebalanceCalculator.RebalancePlan kucoinPlan = calculator.calculatePlan(
                pairs, kucoinTotal, infoMap,
                TradingPair::getKucoinCrypto,
                (p, info) -> info.getMinUsdtKucoin()
        );

        executePlan(bybitPlan, infoMap, true);
        executePlan(kucoinPlan, infoMap, false);

        waitForAllTrades(pairs, bybitPlan, kucoinPlan);

        balanceService.updateAllBalancesAsync().join();
        return true;
    }

    private void executePlan(RebalanceCalculator.RebalancePlan plan, Map<String, MinTradeInfo> infoMap, boolean isBybit) {
        for (Map.Entry<TradingPair, RebalanceCalculator.Action> entry : plan.actions().entrySet()) {
            TradingPair pair = entry.getKey();
            RebalanceCalculator.Action action = entry.getValue();
            double amount = plan.amounts().get(pair);
            MinTradeInfo info = infoMap.get(pair.getSymbol());

            try {
                if (isBybit) {
                    if (action == RebalanceCalculator.Action.BUY) {
                        orderExecutor.bybitBuy(pair, amount, info.getUsdtPrecisionBybit());
                    } else if (action == RebalanceCalculator.Action.SELL) {
                        orderExecutor.bybitSell(pair, amount, info.getCryptoPrecisionBybit());
                    }
                } else {
                    if (action == RebalanceCalculator.Action.BUY) {
                        orderExecutor.kucoinBuy(pair, amount, info.getUsdtPrecisionKucoin());
                    } else if (action == RebalanceCalculator.Action.SELL) {
                        orderExecutor.kucoinSell(pair, amount, info.getCryptoPrecisionKucoin());
                    }
                }
            } catch (Exception e) {
                log.error("[{}] rebalance action failed: {}", pair.getSymbol(), e.getMessage());
            }
        }
    }

    private void waitForAllTrades(List<TradingPair> pairs,
                                  RebalanceCalculator.RebalancePlan bybitPlan,
                                  RebalanceCalculator.RebalancePlan kucoinPlan) {
        int attempts = 0;
        int maxAttempts = 30;
        int logInterval = 5;
        log.info("Waiting for rebalance trades to complete...");

        while (attempts < maxAttempts) {
            boolean allDone = true;
            int pendingCount = 0;

            for (TradingPair pair : pairs) {
                RebalanceCalculator.Action bybitAction = bybitPlan.actions().get(pair);
                if (bybitAction != RebalanceCalculator.Action.NONE) {
                    String orderId = bybitAction == RebalanceCalculator.Action.BUY
                            ? pair.getBybitBuyOrderId()
                            : pair.getBybitSellOrderId();
                    String status = bybitTradeChecker.checkTrade(orderId);
                    if ("InTrade".equals(status)) {
                        allDone = false;
                        pendingCount++;
                    } else if ("Error".equals(status)) {
                        log.error("[{}] bybit trade error orderId={}", pair.getSymbol(), orderId);
                        return;
                    }
                }

                RebalanceCalculator.Action kucoinAction = kucoinPlan.actions().get(pair);
                if (kucoinAction != RebalanceCalculator.Action.NONE) {
                    String orderId = kucoinAction == RebalanceCalculator.Action.BUY
                            ? pair.getKucoinBuyOrderId()
                            : pair.getKucoinSellOrderId();
                    String status = kucoinTradeChecker.checkTrade(orderId);
                    if ("InTrade".equals(status)) {
                        allDone = false;
                        pendingCount++;
                    } else if ("Error".equals(status)) {
                        log.error("[{}] kucoin trade error orderId={}", pair.getSymbol(), orderId);
                        return;
                    }
                }
            }

            if (allDone) {
                log.info("All rebalance trades completed in {} seconds", attempts);
                return;
            }

            if (attempts % logInterval == 0) {
                log.info("Still waiting for {} trades to complete (attempt {}/{})",
                        pendingCount, attempts, maxAttempts);
            }

            attempts++;
            try { Thread.sleep(1000); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        log.error("GlobalRebalance - timeout after {} attempts", maxAttempts);
    }

    public void startInitialRebalance(List<TradingPair> pairs, Map<String, MinTradeInfo> infoMap) {
        pairs.forEach(p -> {
            p.setProcess("REBALANCING");
            p.getCheckInProgress().set(true);
        });

        CompletableFuture.runAsync(() -> {
            try {
                boolean success = executeGlobalRebalance(pairs, infoMap);
                if (success) {
                    pairs.forEach(p -> {
                        p.setProcess("READY_FOR_ALGO");
                        p.getCheckInProgress().set(false);
                        History h = new History();
                        h.setType("R");
                        h.setSymbol(p.getSymbol());
                        h.setBybitPrice((p.getBybitAskPrice() + p.getBybitBidPrice()) / 2);
                        h.setKucoinPrice((p.getKucoinAskPrice() + p.getKucoinBidPrice()) / 2);
                        historyResponse.addToHistory(h);
                    });
                    log.info("Initial rebalance completed successfully");
                } else {
                    pairs.forEach(p -> {
                        p.setProcess("ERROR");
                        p.getCheckInProgress().set(false);
                    });
                    log.error("Initial rebalance failed");
                }
            } catch (Exception e) {
                log.error("Initial rebalance crashed", e);
                pairs.forEach(p -> p.setProcess("ERROR"));
            }
        }, rebalanceExecutor);
    }
}