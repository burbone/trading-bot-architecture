package com.mySelfCode.algo.api.bybit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mySelfCode.algo.cfg.BybitConfig;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class BybitCheckPrice {
    private static final Logger logger = LoggerFactory.getLogger(BybitCheckPrice.class);

    private final WebClient webClient;

    @Getter
    private boolean accept = true;

    @Autowired
    public BybitCheckPrice(WebClient.Builder webClientBuilder, BybitConfig bybitConfig) {
        this.webClient = webClientBuilder
                .baseUrl(bybitConfig.getBaseUrl())
                .build();
    }

    public double[] bybitCheckPrice(String bybitSymbol) {
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v5/market/orderbook")
                            .queryParam("category", "spot")
                            .queryParam("symbol", bybitSymbol)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            double[] prices = extractPricesFromJson(response);
            //logger.debug("Success take prices from bybit for {}: ask={}, bid={}", bybitSymbol, prices[1], prices[0]);

            return prices;
        } catch (Exception e) {
            this.accept = false;
            logger.error("Error take prices from bybit for {}: {}", bybitSymbol, e.getMessage());
            throw new RuntimeException("Error take prices from bybit");
        }
    }

    private double[] extractPricesFromJson(String jsonResponse) throws Exception {
        try {
            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            int retCode = root.get("retCode").getAsInt();

            if (retCode != 0) {
                String retMsg = root.get("retMsg").getAsString();
                throw new RuntimeException("Bybit API error: " + retMsg);
            }

            JsonObject result = root.getAsJsonObject("result");

            JsonArray bidArray = result.getAsJsonArray("b");
            double bid = bidArray.get(0).getAsJsonArray().get(0).getAsDouble();

            JsonArray askArray = result.getAsJsonArray("a");
            double ask = askArray.get(0).getAsJsonArray().get(0).getAsDouble();

            return new double[]{bid, ask};

        } catch (Exception e) {
            logger.error("Error for take prices bybit: {}", e.getMessage());
            throw new Exception("Error parsing prices: " + e.getMessage(), e);
        }
    }
}