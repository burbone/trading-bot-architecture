package com.mySelfCode.algo.controller;

import com.mySelfCode.algo.cfg.BotConfig;
import com.mySelfCode.algo.dto.Status;
import com.mySelfCode.algo.dto.TradingPairRegistry;
import com.mySelfCode.algo.entity.StopRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StopController {

    private static final Logger log = LoggerFactory.getLogger(StopController.class);

    private final Status status;
    private final TradingPairRegistry registry;
    private final BotConfig botConfig;

    @PostMapping("/stop")
    public ResponseEntity<?> stopAlgorithm(@RequestBody StopRequest request) {
        try {
            if (!botConfig.getPassword().equals(request.getPassword())) {
                log.error("Wrong password");
                return ResponseEntity.status(403).body("Wrong password");
            }

            log.info("Stopping algo");
            status.setStatus(false);
            status.setStatusForAlgo(false);
            status.setGlobalRebalanceDone(false);
            registry.clear();

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error stopping algo", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}