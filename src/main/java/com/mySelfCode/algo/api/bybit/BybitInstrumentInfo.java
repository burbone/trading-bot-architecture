package com.mySelfCode.algo.api.bybit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mySelfCode.algo.cfg.BybitConfig;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class BybitInstrumentInfo {
    private static final Logger logger = LoggerFactory.getLogger(BybitInstrumentInfo.class);

    private final WebClient webClient;
    private final BybitConfig bybitConfig;

    @Getter
    private boolean accept = true;

    @Autowired
    public BybitInstrumentInfo(WebClient.Builder webClientBuilder, BybitConfig bybitConfig) {
        this.bybitConfig = bybitConfig;
        this.webClient = webClientBuilder
                .baseUrl(bybitConfig.getBaseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public double[] getMinTradeInfo(String symbol) {
        symbol = symbol.replaceAll(" ", "");

        try {
            String finalSymbol = symbol;
            String jsonResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v5/market/instruments-info")
                            .queryParam("category", "spot")
                            .queryParam("symbol", finalSymbol)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return extractMinTradeInfo(jsonResponse);

        } catch (Exception e) {
            this.accept = false;
            logger.error("Error getting instrument info for {}: {}", symbol, e.getMessage());
            throw new RuntimeException("Error getting instrument info for " + symbol + ": " + e.getMessage(), e);
        }
    }

    private double[] extractMinTradeInfo(String jsonResponse) {
        try {
            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            int retCode = root.get("retCode").getAsInt();

            if (retCode != 0) {
                String retMsg = root.get("retMsg").getAsString();
                throw new RuntimeException("Bybit API error: " + retMsg);
            }

            JsonObject result = root.getAsJsonObject("result");
            JsonArray list = result.getAsJsonArray("list");

            if (list.size() > 0) {
                JsonObject instrument = list.get(0).getAsJsonObject();
                JsonObject lotSizeFilter = instrument.getAsJsonObject("lotSizeFilter");

                double minOrderQty = Double.parseDouble(lotSizeFilter.get("minOrderQty").getAsString());
                double minOrderAmt = Double.parseDouble(lotSizeFilter.get("minOrderAmt").getAsString());
                double basePrecision = Double.parseDouble(lotSizeFilter.get("basePrecision").getAsString());
                double quotePrecision = Double.parseDouble(lotSizeFilter.get("quotePrecision").getAsString());

                logger.info("Bybit min trade info - minCrypto: {}, minUsdt: {}, precisionCrypto: {}, precisionUsdt: {}",
                        minOrderQty, minOrderAmt, basePrecision, quotePrecision);

                return new double[]{minOrderQty, minOrderAmt, basePrecision, quotePrecision};
            }

            throw new RuntimeException("No instrument data found in response");

        } catch (Exception e) {
            throw new RuntimeException("Error parsing Bybit instrument info: " + e.getMessage());
        }
    }
}