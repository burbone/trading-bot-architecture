package com.mySelfCode.algo.service.algorithm;

import com.mySelfCode.algo.dto.*;
import com.mySelfCode.algo.entity.HistoryResponse;
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

    public void process(TradingPair pair, MinTradeInfo info) {
        switch (pair.getProcess()) {
            case "READY_FOR_ALGO" -> handleReady(pair, info);
            case "IN_ALGO" -> handleInAlgo(pair);
            case "ERROR" -> handleError(pair);
            case "REBALANCING" -> { /* no log */}
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
            pair.getWaitAlgoLog().set(60);
            if (midBybit > 0) {
                double diffPercent = Math.abs(midBybit - midKucoin) / midBybit * 100;
                log.info("[{}] IN_ALGO waiting | bybit={} kucoin={} diff={}%",
                        pair.getSymbol(), midBybit, midKucoin, diffPercent);
            }
        } else {
            log.trace("[{}] IN_ALGO check: bybitMid={} kucoinMid={}",
                    pair.getSymbol(), midBybit, midKucoin);
        }

        if (midBybit > 0 && Math.abs(midBybit - midKucoin) / midBybit * 100 <= 0.001) {
            addHistoryRecord(pair, "W");
            pair.setProcess("READY_TO_EXIT_FROM_ALGO");
            registry.unlock();
            log.info("[{}] EXIT: prices converged", pair.getSymbol());
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