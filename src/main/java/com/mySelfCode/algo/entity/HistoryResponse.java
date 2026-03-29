package com.mySelfCode.algo.entity;

import com.mySelfCode.algo.dto.History;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Setter
@Component
public class HistoryResponse {
    private List<History> historyList = new CopyOnWriteArrayList<>();

    public synchronized void addToHistory(History history){
        if(historyList.size() >= 10) {
            historyList.remove(0);
            historyList.add(history);
        } else {
            historyList.add(history);
        }
    }
}
