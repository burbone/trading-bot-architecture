package com.mySelfCode.algo.api.bybit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mySelfCode.algo.cfg.BybitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class BybitTimeService {

    private final BybitConfig bybitConfig;
    private final WebClient.Builder webClientBuilder;
    private final AtomicLong lastServerTime = new AtomicLong(System.currentTimeMillis());
    private volatile long lastFetchTime = 0;
    private static final long CACHE_DURATION_MS = 10_000;

    public long getServerTime() {
        long now = System.currentTimeMillis();
        if (now - lastFetchTime > CACHE_DURATION_MS) {
            fetchServerTime();
            lastFetchTime = now;
        }
        return lastServerTime.get() + (System.currentTimeMillis() - lastFetchTime);
    }

    private void fetchServerTime() {
        WebClient webClient = webClientBuilder.baseUrl(bybitConfig.getBaseUrl()).build();
        try {
            String response = webClient.get()
                    .uri("/v5/market/time")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonObject root = JsonParser.parseString(response).getAsJsonObject();
            int retCode = root.get("retCode").getAsInt();
            if (retCode == 0) {
                JsonObject result = root.getAsJsonObject("result");
                long serverTime = Long.parseLong(result.get("timeSecond").getAsString()) * 1000;
                lastServerTime.set(serverTime);
            } else {
                log.warn("Failed to fetch Bybit server time: {}", root.get("retMsg").getAsString());
            }
        } catch (Exception e) {
            log.error("Error fetching Bybit server time", e);
        }
    }
}