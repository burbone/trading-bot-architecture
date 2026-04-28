package com.mySelfCode.algo.controller;

import com.mySelfCode.algo.cfg.BotConfig;
import com.mySelfCode.algo.dto.Status;
import com.mySelfCode.algo.dto.TradingPair;
import com.mySelfCode.algo.dto.TradingPairRegistry;
import com.mySelfCode.algo.entity.StartRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StarterController {

    private final BotConfig botConfig;
    private final TradingPairRegistry registry;
    private final Status status;

    @PostMapping("/starter")
    public ResponseEntity<?> start(@RequestBody StartRequest request) {
        if (!botConfig.getPassword().isEmpty() && !botConfig.getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(403).body("Wrong password");
        }

        List<TradingPair> pairs = request.getPairs().stream()
                .map(p -> new TradingPair(p.getSymbol(), Double.parseDouble(p.getPercent())))
                .toList();

        registry.setPairs(pairs);

        status.setStatus(true);
        status.setStatusForAlgo(true);

        return ResponseEntity.ok("Started (demo)");
    }
}