package com.mySelfCode.algo.service.algorithm;

import com.mySelfCode.algo.dto.MinTradeInfo;
import com.mySelfCode.algo.dto.Status;
import com.mySelfCode.algo.dto.TradingPair;
import com.mySelfCode.algo.dto.TradingPairRegistry;
import com.mySelfCode.algo.service.rebalance.GlobalRebalanceOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlgorithmEngine {

    private final TradingPairRegistry registry;
    private final TradingStateMachine stateMachine;
    private final GlobalRebalanceOrchestrator rebalanceOrchestrator;
    private final Status status;

    @Setter
    private volatile Map<String, MinTradeInfo> minTradeInfoMap;

    private long lastStatsLog = 0;
    private static final long STATS_LOG_INTERVAL = 300_000;

    public void tick() {
        if (!status.isStatusForAlgo() || minTradeInfoMap == null) return;

        long now = System.currentTimeMillis();
        if (now - lastStatsLog > STATS_LOG_INTERVAL) {
            logStats();
            lastStatsLog = now;
        }

        for (TradingPair pair : registry.getPairs()) {
            MinTradeInfo info = minTradeInfoMap.get(pair.getSymbol());
            if (info == null) continue;

            if ("READY_TO_EXIT_FROM_ALGO".equals(pair.getProcess())) {
                rebalanceOrchestrator.startGlobalRebalanceIfNeeded(pair, minTradeInfoMap);
                continue;
            }

            stateMachine.process(pair, info);
        }
    }

    private void logStats() {
        Map<String, Integer> stateCounts = new HashMap<>();
        for (TradingPair p : registry.getPairs()) {
            stateCounts.merge(p.getProcess(), 1, Integer::sum);
        }
        log.info("Algorithm status - pairs: {}, states: {}", registry.getPairs().size(), stateCounts);
    }
}