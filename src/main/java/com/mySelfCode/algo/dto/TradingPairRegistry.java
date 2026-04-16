package com.mySelfCode.algo.dto;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Getter
public class TradingPairRegistry {
    private final List<TradingPair> pairs = new CopyOnWriteArrayList<>();
    private final AtomicReference<String> activePairSymbol = new AtomicReference<>(null);

    public void setPairs(List<TradingPair> newPairs) {
        pairs.clear();
        pairs.addAll(newPairs);
    }

    public boolean tryLock(String symbol) {
        return activePairSymbol.compareAndSet(null, symbol);
    }

    public void unlock() {
        activePairSymbol.set(null);
    }

    public boolean isLocked() {
        return activePairSymbol.get() != null;
    }

    public void clear() {
        pairs.clear();
        activePairSymbol.set(null);
    }
}