package com.mySelfCode.algo.service.scheduler;

import com.mySelfCode.algo.dto.Status;
import com.mySelfCode.algo.service.algorithm.AlgorithmEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradingScheduler {

    private final Status status;
    private final AlgorithmEngine algorithmEngine;

    @Scheduled(fixedRate = 1000)
    public void runAlgorithm() {
        if (!status.isStatusForAlgo()) return;
        algorithmEngine.tick();
    }
}