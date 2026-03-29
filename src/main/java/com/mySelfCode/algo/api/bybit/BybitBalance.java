package com.mySelfCode.algo.api.bybit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.mySelfCode.algo.cfg.BybitConfig;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Formatter;

@Service
public class BybitBalance {
    private static final Logger logger = LoggerFactory.getLogger(BybitBalance.class);

    private final WebClient webClient;
    private final BybitConfig bybitConfig;

    @Getter
    private boolean accept = true;

    @Autowired
    public BybitBalance(WebClient.Builder webClientBuilder, BybitConfig bybitConfig) {
        this.bybitConfig = bybitConfig;
        this.webClient = webClientBuilder
                .baseUrl(bybitConfig.getBaseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public double getWalletBalance(String symbol) {
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String recvWindow = "5000";
        String accountType = "UNIFIED";

        String queryString = String.format("accountType=%s&coin=%s", accountType, symbol.toUpperCase());
        String signature = generateSignature(timestamp, bybitConfig.getKey(), recvWindow, queryString);

        try {
            String jsonResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v5/account/wallet-balance")
                            .queryParam("accountType", accountType)
                            .queryParam("coin", symbol)
                            .build())
                    .header("X-BAPI-API-KEY", bybitConfig.getKey())
                    .header("X-BAPI-TIMESTAMP", timestamp)
                    .header("X-BAPI-RECV-WINDOW", recvWindow)
                    .header("X-BAPI-SIGN", signature)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            double balance = extractBalanceFromJson(jsonResponse, symbol.toUpperCase());
            return balance;

        } catch (Exception e) {
            this.accept = false;
            logger.error("Error while take bybit balance {}: {}", symbol, e.getMessage());
            throw new RuntimeException("Error getting balance for " + symbol + ": " + e.getMessage(), e);
        }
    }

    public double getCoinBalance(String symbol) {
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String recvWindow = "5000";
        String accountType = "UNIFIED";

        String queryString = String.format("accountType=%s&coin=%s", accountType, symbol.toUpperCase());
        String signature = generateSignature(timestamp, bybitConfig.getKey(), recvWindow, queryString);

        try {
            String jsonResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v5/account/wallet-balance")
                            .queryParam("accountType", accountType)
                            .queryParam("coin", symbol.toUpperCase())
                            .build())
                    .header("X-BAPI-API-KEY", bybitConfig.getKey())
                    .header("X-BAPI-TIMESTAMP", timestamp)
                    .header("X-BAPI-RECV-WINDOW", recvWindow)
                    .header("X-BAPI-SIGN", signature)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            double coinBalance = extractBalanceFromJson(jsonResponse, symbol.toUpperCase());
            return coinBalance;

        } catch (Exception e) {
            this.accept = false;
            logger.error("Error take coin balance on bybit {}: {}", symbol, e.getMessage());
            throw new RuntimeException("Error getting coin balance for " + symbol + ": " + e.getMessage(), e);
        }
    }

    private double extractBalanceFromJson(String jsonResponse, String targetCoin) {
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
                JsonObject account = list.get(0).getAsJsonObject();
                JsonArray coins = account.getAsJsonArray("coin");

                for (int i = 0; i < coins.size(); i++) {
                    JsonObject coin = coins.get(i).getAsJsonObject();
                    if (coin.get("coin").getAsString().equals(targetCoin)) {
                        if (coin.get("walletBalance") != null && !coin.get("walletBalance").isJsonNull()) {
                            String walletBalanceStr = coin.get("walletBalance").getAsString();
                            if (walletBalanceStr != null && !walletBalanceStr.trim().isEmpty()) {
                                try {
                                    return Double.parseDouble(walletBalanceStr);
                                } catch (NumberFormatException e) {
                                    logger.warn("Cannot parse walletBalance on bybit '{}' for coin {}", walletBalanceStr, targetCoin);
                                    return 0.0;
                                }
                            }
                        }
                        return 0.0;
                    }
                }
            }

            return 0.0;

        } catch (Exception e) {
            throw new RuntimeException("Error parsing Bybit response: " + e.getMessage());
        }
    }

    private String generateSignature(String timestamp, String apiKey, String recvWindow, String queryString) {
        try {
            String payload = timestamp + apiKey + recvWindow + queryString;
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(bybitConfig.getSecret().getBytes(), "HmacSHA256");
            hmac.init(secretKeySpec);
            byte[] hash = hmac.doFinal(payload.getBytes());
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Ошибка при генерации подписи", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        try (Formatter formatter = new Formatter()) {
            for (byte b : bytes) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }
}