package com.mySelfCode.algo.controller;

import com.mySelfCode.algo.dto.CompletedTrade;
import com.mySelfCode.algo.entity.CompletedTradeStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class CompletedTradeController {

    private final CompletedTradeStorage storage;

    @GetMapping("/completed")
    public ResponseEntity<List<CompletedTrade>> getCompletedTrades() {
        return ResponseEntity.ok(storage.getTrades());
    }
}