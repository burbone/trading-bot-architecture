package com.mySelfCode.algo.controller;

import com.mySelfCode.algo.cfg.BotConfig;
import com.mySelfCode.algo.dto.Status;
import com.mySelfCode.algo.dto.TradingPairRegistry;
import com.mySelfCode.algo.entity.StopRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StopController {

    private final BotConfig botConfig;
    private final Status status;
    private final TradingPairRegistry registry;

    @PostMapping("/stop")
    public ResponseEntity<?> stop(@RequestBody StopRequest request) {
        if (!botConfig.getPassword().isEmpty() && !botConfig.getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(403).body("Wrong password");
        }

        status.setStatus(false);
        status.setStatusForAlgo(false);
        registry.clear();

        return ResponseEntity.ok("Stopped");
    }
}