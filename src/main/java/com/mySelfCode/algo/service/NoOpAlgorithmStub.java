package com.mySelfCode.algo.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NoOpAlgorithmStub {

    @Scheduled(fixedRate = 1000)
    public void tick() {
        // Trading logic removed (NDA)
    }
}