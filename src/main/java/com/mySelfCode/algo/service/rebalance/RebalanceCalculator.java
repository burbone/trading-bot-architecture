package com.mySelfCode.algo.service.rebalance;

import com.mySelfCode.algo.dto.MinTradeInfo;
import com.mySelfCode.algo.dto.TradingPair;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RebalanceCalculator {

    public enum Action { BUY, SELL, NONE }

    public record RebalancePlan(double slotSize, Map<TradingPair, Action> actions,
                                Map<TradingPair, Double> amounts) {}

    public RebalancePlan calculatePlan(List<TradingPair> pairs, double totalUsdt,
                                       Map<String, MinTradeInfo> infoMap,
                                       java.util.function.Function<TradingPair, Double> valueExtractor,
                                       java.util.function.BiFunction<TradingPair, MinTradeInfo, Double> minUsdtExtractor) {
        int slots = pairs.size() + 1;
        double slotSize = totalUsdt / slots;

        Map<TradingPair, Action> actions = new HashMap<>();
        Map<TradingPair, Double> amounts = new HashMap<>();

        for (TradingPair pair : pairs) {
            MinTradeInfo info = infoMap.get(pair.getSymbol());
            double currentValue = valueExtractor.apply(pair);
            double diff = slotSize - currentValue;
            double minUsdt = minUsdtExtractor.apply(pair, info);

            Action action;
            if (Math.abs(diff) < minUsdt) {
                action = Action.NONE;
            } else {
                action = diff > 0 ? Action.BUY : Action.SELL;
            }
            actions.put(pair, action);
            amounts.put(pair, Math.abs(diff));
        }

        return new RebalancePlan(slotSize, actions, amounts);
    }
}