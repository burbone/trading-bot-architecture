package com.mySelfCode.algo.service.algorithm;

import com.mySelfCode.algo.cfg.BotConfig;
import com.mySelfCode.algo.dto.*;
import com.mySelfCode.algo.entity.CompletedTradeStorage;
import com.mySelfCode.algo.entity.HistoryResponse;
import com.mySelfCode.algo.service.analytics.ProfitCalculator;
import com.mySelfCode.algo.service.trading.PairTradeService;
import com.mySelfCode.algo.service.trading.TradeStatusChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradingStateMachine {

    private final PairTradeService pairTradeService;
    private final TradeStatusChecker statusChecker;
    private final EntryConditionEvaluator entryEvaluator;
    private final HistoryResponse historyResponse;
    private final TradingPairRegistry registry;
    private final BotConfig botConfig;
    private final ProfitCalculator profitCalculator;
    private final CompletedTradeStorage completedTradeStorage;
    private final ExchangeUsdt exchangeUsdt;

    public void process(TradingPair pair, MinTradeInfo info) {
        switch (pair.getProcess()) {
            case "READY_FOR_ALGO" -> handleReady(pair, info);
            case "IN_ALGO" -> handleInAlgo(pair);
            case "ERROR" -> handleError(pair);
            case "REBALANCING" -> { /* no log */ }
        }
    }

    private void handleReady(TradingPair pair, MinTradeInfo info) {
        if (pair.getCheckInProgress().get()) {
            checkEntryConfirmation(pair);
            return;
        }

        EntryConditionEvaluator.EntryOpportunity opp = entryEvaluator.evaluate(pair, info);
        if (!opp.valid()) return;

        if (!registry.tryLock(pair.getSymbol())) return;

        if (opp.isBybitToKucoin()) {
            pair.setBuyOnBybit(false);
            pairTradeService.sellBybitBuyKucoin(pair, opp.sellAmountUsdt(), opp.buyAmountUsdt(), info);
        } else {
            pair.setBuyOnBybit(true);
            pairTradeService.buyBybitSellKucoin(pair, opp.buyAmountUsdt(), opp.sellAmountUsdt(), info);
        }

        pair.setEntryBybitPrice((pair.getBybitAskPrice() + pair.getBybitBidPrice()) / 2);
        pair.setEntryKucoinPrice((pair.getKucoinAskPrice() + pair.getKucoinBidPrice()) / 2);

        pair.getWait().set(15);
        pair.getCheckInProgress().set(true);
        log.info("[{}] entry orders placed: {}", pair.getSymbol(), opp.direction());
    }

    private void checkEntryConfirmation(TradingPair pair) {
        int remaining = pair.getWait().decrementAndGet();

        String bybitStatus = pair.isBuyOnBybit()
                ? statusChecker.checkBybit(pair.getBybitBuyOrderId())
                : statusChecker.checkBybit(pair.getBybitSellOrderId());

        String kucoinStatus = pair.isBuyOnBybit()
                ? statusChecker.checkKucoin(pair.getKucoinSellOrderId())
                : statusChecker.checkKucoin(pair.getKucoinBuyOrderId());

        if ("Done".equals(bybitStatus) && "Done".equals(kucoinStatus)) {
            pair.getCheckInProgress().set(false);
            pair.setProcess("IN_ALGO");
            pair.setEntryBybitPrice((pair.getBybitAskPrice() + pair.getBybitBidPrice()) / 2);
            pair.setEntryKucoinPrice((pair.getKucoinAskPrice() + pair.getKucoinBidPrice()) / 2);
            double totalBalance = exchangeUsdt.getBybitUsdt() + exchangeUsdt.getKucoinUsdt() +
                    registry.getPairs().stream().mapToDouble(p -> p.getBybitCrypto() + p.getKucoinCrypto()).sum();
            pair.setTotalBalanceBefore(totalBalance);
            addHistoryRecord(pair, "D");
            log.info("[{}] entered algo", pair.getSymbol());
        } else if ("Error".equals(bybitStatus) || "Error".equals(kucoinStatus) || remaining <= 0) {
            log.error("[{}] entry confirmation failed: bybit={}, kucoin={}",
                    pair.getSymbol(), bybitStatus, kucoinStatus);
            pair.getCheckInProgress().set(false);
            pair.setProcess("ERROR");
            registry.unlock();
        }
    }

    private void handleInAlgo(TradingPair pair) {
        double midBybit = (pair.getBybitAskPrice() + pair.getBybitBidPrice()) / 2;
        double midKucoin = (pair.getKucoinAskPrice() + pair.getKucoinBidPrice()) / 2;

        int logTick = pair.getWaitAlgoLog().decrementAndGet();
        if (logTick <= 0) {
            pair.getWaitAlgoLog().set(300);
            if (midBybit > 0) {
                double diffPercent = Math.abs(midBybit - midKucoin) / midBybit * 100;
                log.info("[{}] IN_ALGO waiting | bybit={} kucoin={} diff={}%",
                        pair.getSymbol(), midBybit, midKucoin, diffPercent);
            }
        } else {
            log.trace("[{}] IN_ALGO check: bybitMid={} kucoinMid={}",
                    pair.getSymbol(), midBybit, midKucoin);
        }

        double threshold = 1 + botConfig.getReverseSpread();
        boolean exitCondition = false;

        if (pair.isBuyOnBybit()) {
            if (pair.getBybitBidPrice() > pair.getKucoinAskPrice() * threshold) {
                exitCondition = true;
            }
        } else {
            if (pair.getKucoinBidPrice() > pair.getBybitAskPrice() * threshold) {
                exitCondition = true;
            }
        }

        if (exitCondition) {
            addHistoryRecord(pair, "W");
            pair.setProcess("READY_TO_EXIT_FROM_ALGO");
            registry.unlock();
            double exitBybit = (pair.getBybitAskPrice() + pair.getBybitBidPrice()) / 2;
            double exitKucoin = (pair.getKucoinAskPrice() + pair.getKucoinBidPrice()) / 2;
            CompletedTrade trade = profitCalculator.calculate(pair, pair.getEntryBybitPrice(),
                    pair.getEntryKucoinPrice(), exitBybit, exitKucoin, pair.getTotalBalanceBefore());
            completedTradeStorage.add(trade);
            log.info("[{}] EXIT: reverse spread reached (threshold 0.2%)", pair.getSymbol());
        }
    }

    private void handleError(TradingPair pair) {
    }

    private void addHistoryRecord(TradingPair pair, String type) {
        History h = new History();
        h.setType(type);
        h.setSymbol(pair.getSymbol());
        h.setBybitPrice((pair.getBybitAskPrice() + pair.getBybitBidPrice()) / 2);
        h.setKucoinPrice((pair.getKucoinAskPrice() + pair.getKucoinBidPrice()) / 2);
        historyResponse.addToHistory(h);
    }
}