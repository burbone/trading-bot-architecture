package com.mySelfCode.algo.api.kucoin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mySelfCode.algo.cfg.BotConfig;
import com.mySelfCode.algo.cfg.KucoinConfig;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class KucoinInstrumentInfo {
    private static final Logger logger = LoggerFactory.getLogger(KucoinInstrumentInfo.class);

    private final WebClient webClient;
    private final BotConfig botConfig;

    @Getter
    private boolean accept = true;

    @Autowired
    public KucoinInstrumentInfo(WebClient.Builder webClientBuilder, KucoinConfig kucoinConfig, BotConfig botConfig) {
        this.botConfig = botConfig;
        this.webClient = webClientBuilder
                .baseUrl(kucoinConfig.getBaseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public double[] getMinTradeInfo(String symbol) {
        symbol = symbol.replaceAll(" ", "-");
        if (botConfig.isSimulationMode()) {
            return new double[]{1.0, 0.1, 0.0001, 0.0001};
        }

        try {
            String jsonResponse = webClient.get()
                    .uri("/api/v2/symbols/" + symbol)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return extractMinTradeInfo(jsonResponse, symbol);

        } catch (Exception e) {
            this.accept = false;
            logger.error("Error getting instrument info for {}: {}", symbol, e.getMessage());
            throw new RuntimeException("Error getting instrument info for " + symbol + ": " + e.getMessage(), e);
        }
    }

    private double[] extractMinTradeInfo(String jsonResponse, String targetSymbol) {
        try {
            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            String code = root.get("code").getAsString();

            if (!"200000".equals(code)) {
                throw new RuntimeException("KuCoin API error: " + code);
            }

            JsonObject data = root.getAsJsonObject("data");

            double baseMinSize = Double.parseDouble(data.get("baseMinSize").getAsString());
            double quoteMinSize = Double.parseDouble(data.get("quoteMinSize").getAsString());
            double baseIncrement = Double.parseDouble(data.get("baseIncrement").getAsString());
            double quoteIncrement = Double.parseDouble(data.get("quoteIncrement").getAsString());

            return new double[]{baseMinSize, quoteMinSize, baseIncrement, quoteIncrement};
        } catch (Exception e) {
            throw new RuntimeException("Error parsing KuCoin instrument info: " + e.getMessage());
        }
    }
}