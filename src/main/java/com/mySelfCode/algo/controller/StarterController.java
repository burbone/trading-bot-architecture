package com.mySelfCode.algo.controller;

import com.mySelfCode.algo.cfg.BotConfig;
import com.mySelfCode.algo.dto.*;
import com.mySelfCode.algo.entity.StartRequest;
import com.mySelfCode.algo.service.algorithm.AlgorithmEngine;
import com.mySelfCode.algo.service.rebalance.GlobalRebalanceOrchestrator;
import com.mySelfCode.algo.service.rebalance.RebalanceCalculator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StarterController {

    private static final Logger log = LoggerFactory.getLogger(StarterController.class);

    private final Status status;
    private final TradingPairRegistry registry;
    private final BotConfig botConfig;
    private final GlobalRebalanceOrchestrator globalRebalanceOrchestrator;
    private final GlobalRebalanceOrchestrator rebalanceOrchestrator;
    private final AlgorithmEngine algorithmEngine;

    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestBody StartRequest request) {
        if (!botConfig.getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(403).body("Wrong password");
        }

        List<TradingPair> pairs = request.getPairs().stream()
                .map(p -> new TradingPair(p.getSymbol(), Double.parseDouble(p.getPercent())
                        + ((botConfig.getKucoinFee() + botConfig.getBybitFee()) * 100)
                ))
                .toList();
        registry.setPairs(pairs);

        Map<String, MinTradeInfo> infoMap = globalRebalanceOrchestrator.buildMinTradeInfoMap(pairs);
        algorithmEngine.setMinTradeInfoMap(infoMap);

        status.setStatus(true);
        status.setStatusForAlgo(true);
        status.setGlobalRebalanceDone(false);

        rebalanceOrchestrator.startInitialRebalance(pairs, infoMap);

        return ResponseEntity.ok("Started");
    }
}