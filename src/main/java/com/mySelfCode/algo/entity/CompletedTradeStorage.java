package com.mySelfCode.algo.entity;

import com.mySelfCode.algo.dto.CompletedTrade;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Component
public class CompletedTradeStorage {
    private final List<CompletedTrade> trades = new ArrayList<>();

    public synchronized void add(CompletedTrade trade) {
        if (trades.size() >= 100) {
            trades.remove(0);
        }
        trades.add(trade);
    }

    public synchronized void clear() {
        trades.clear();
    }
}