package com.mySelfCode.algo.api.kucoin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mySelfCode.algo.cfg.KucoinConfig;
import com.mySelfCode.algo.dto.StarterInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class KucoinCheckPrice {
    private static final Logger logger = LoggerFactory.getLogger(KucoinCheckPrice.class);

    private final WebClient webClient;
    private final StarterInfo starterInfo;

    @Getter
    private boolean accept = true;

    @Autowired
    public KucoinCheckPrice(WebClient.Builder webClientBuilder, KucoinConfig kucoinConfig, StarterInfo starterInfo) {
        this.starterInfo = starterInfo;
        this.webClient = webClientBuilder
                .baseUrl(kucoinConfig.getBaseUrl())
                .build();
    }

    public double[] kucoinCheckPrice() {
        String symbol = starterInfo.getSymbol().replaceAll(" ", "-");
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/market/orderbook/level1")
                            .queryParam("symbol", symbol)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            double[] prices = extractPricesFromJson(response);
            logger.debug("Success take prices from kucoin {}: ask={}, bid={}", symbol, prices[1], prices[0]);

            return prices;
        } catch (Exception e) {
            this.accept = false;
            logger.error("Error take prices from kucoin for {}: ", symbol + e.getMessage());
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