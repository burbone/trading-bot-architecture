package com.mySelfCode.algo.api.kucoin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mySelfCode.algo.cfg.BotConfig;
import com.mySelfCode.algo.cfg.KucoinConfig;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class KucoinCheckPrice {
    private static final Logger logger = LoggerFactory.getLogger(KucoinCheckPrice.class);

    private final WebClient webClient;
    private final BotConfig botConfig;

    @Getter
    private boolean accept = true;

    @Autowired
    public KucoinCheckPrice(WebClient.Builder webClientBuilder, KucoinConfig kucoinConfig, BotConfig botConfig) {
        this.botConfig = botConfig;
        this.webClient = webClientBuilder
                .baseUrl(kucoinConfig.getBaseUrl())
                .build();
    }

    public double[] kucoinCheckPrice(String kucoinSymbol) {
        if (botConfig.isSimulationMode()) {
            double base = 100.0 + Math.random() * 10;
            return new double[]{base * 0.999, base * 1.001};
        }
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/market/orderbook/level1")
                            .queryParam("symbol", kucoinSymbol)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return extractPricesFromJson(response);
        } catch (Exception e) {
            this.accept = false;
            logger.error("Error take prices from kucoin for {}: {}", kucoinSymbol, e.getMessage());
            throw new RuntimeException("Error take kucoin prices");
        }
    }

    private double[] extractPricesFromJson(String jsonResponse) throws Exception {
        try {
            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            String code = root.get("code").getAsString();

            if (!"200000".equals(code)) {
                throw new RuntimeException("KuCoin API error: " + code);
            }

            JsonObject data = root.getAsJsonObject("data");

            double bid = data.get("bestBid").getAsDouble();
            double ask = data.get("bestAsk").getAsDouble();

            return new double[]{bid, ask};

        } catch (Exception e) {
            logger.error("Error for take prices from kucoin: {}", e.getMessage());
            throw new Exception("Error parsing prices: " + e.getMessage(), e);
        }
    }
}