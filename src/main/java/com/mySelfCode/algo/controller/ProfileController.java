package com.mySelfCode.algo.controller;

import com.mySelfCode.algo.dto.ExchangeUsdt;
import com.mySelfCode.algo.dto.TradingPair;
import com.mySelfCode.algo.dto.TradingPairRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final TradingPairRegistry registry;
    private final ExchangeUsdt exchangeUsdt;

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> allProfiles() {
        List<TradingPair> pairs = registry.getPairs();
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("bybitUsdt", exchangeUsdt.getBybitUsdt());
        result.put("kucoinUsdt", exchangeUsdt.getKucoinUsdt());

        List<Map<String, Object>> pairsList = pairs.stream().map(pair -> {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("symbol", pair.getSymbol());
            p.put("process", pair.getProcess());

            Map<String, Object> bybit = new LinkedHashMap<>();
            bybit.put("usdt", exchangeUsdt.getBybitUsdt());
            bybit.put("crypto", pair.getBybitCrypto());
            bybit.put("coins", pair.getBybitCoins());
            p.put("bybit", bybit);

            Map<String, Object> kucoin = new LinkedHashMap<>();
            kucoin.put("usdt", exchangeUsdt.getKucoinUsdt());
            kucoin.put("crypto", pair.getKucoinCrypto());
            kucoin.put("coins", pair.getKucoinCoins());
            p.put("kucoin", kucoin);

            return p;
        }).toList();

        result.put("pairs", pairsList);
        return ResponseEntity.ok(result);
    }
}