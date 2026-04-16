package com.mySelfCode.algo.service.rebalance;

import com.mySelfCode.algo.dto.MinTradeInfo;
import com.mySelfCode.algo.dto.TradingPair;
import com.mySelfCode.algo.service.trading.OrderExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RebalanceExecutor {

    private final OrderExecutor orderExecutor;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public CompletableFuture<Void> execute(RebalanceCalculator.RebalancePlan plan,
                                           Map<String, MinTradeInfo> infoMap,
                                           java.util.function.BiConsumer<TradingPair, RebalanceCalculator.Action> actionExecutor) {
        var futures = plan.actions().entrySet().stream()
                .map(entry -> CompletableFuture.runAsync(() -> {
                    TradingPair pair = entry.getKey();
                    RebalanceCalculator.Action action = entry.getValue();
                    if (action != RebalanceCalculator.Action.NONE) {
                        actionExecutor.accept(pair, action);
                    }
                }, executor))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures);
    }
}