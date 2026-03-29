package com.mySelfCode.algo.controller;

import com.mySelfCode.algo.dto.History;
import com.mySelfCode.algo.entity.HistoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryResponse historyResponse;

    @GetMapping("/history")
    public ResponseEntity<List<History>> history() {
        return ResponseEntity.ok(historyResponse.getHistoryList());
    }
}