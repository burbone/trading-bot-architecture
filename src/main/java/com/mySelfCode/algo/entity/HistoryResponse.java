package com.mySelfCode.algo.entity;

import com.mySelfCode.algo.dto.History;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Component
public class HistoryResponse {
    private final List<History> historyList = new CopyOnWriteArrayList<>();

    public synchronized void addToHistory(History history) {
        if (historyList.size() >= 100) {
            historyList.remove(0);
        }
        historyList.add(history);
    }
}
