package com.mySelfCode.algo.api.bybit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mySelfCode.algo.cfg.BotConfig;
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
import java.util.HashMap;
import java.util.Map;

@Service
public class BybitBalance {
    private static final Logger logger = LoggerFactory.getLogger(BybitBalance.class);

    private final WebClient webClient;
    private final BybitConfig bybitConfig;
    private final BotConfig botConfig;

    @Getter
    private boolean accept = true;

    @Autowired
    public BybitBalance(WebClient.Builder webClientBuilder, BybitConfig bybitConfig, BotConfig botConfig) {
        this.bybitConfig = bybitConfig;
        this.botConfig = botConfig;
        this.webClient = webClientBuilder
                .baseUrl(bybitConfig.getBaseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public double getBalance(String symbol) {
        if (botConfig.isSimulationMode()) {
            return 100.0;
        }
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

            return extractBalanceFromJson(jsonResponse, symbol.toUpperCase());

        } catch (Exception e) {
            this.accept = false;
            logger.error("Error while take bybit balance {}: {}", symbol, e.getMessage());
            throw new RuntimeException("Error getting balance for " + symbol + ": " + e.getMessage(), e);
        }
    }

    public Map<String, Double> getAllCoins() {
        if (botConfig.isSimulationMode()) {
            Map<String, Double> mock = new HashMap<>();
            mock.put("USDT", 100.0);
            return mock;
        }
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String recvWindow = "5000";
        String accountType = "UNIFIED";

        String queryString = String.format("accountType=%s", accountType);
        String signature = generateSignature(timestamp, bybitConfig.getKey(), recvWindow, queryString);

        try {
            String jsonResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v5/account/wallet-balance")
                            .queryParam("accountType", accountType)
                            .build())
                    .header("X-BAPI-API-KEY", bybitConfig.getKey())
                    .header("X-BAPI-TIMESTAMP", timestamp)
                    .header("X-BAPI-RECV-WINDOW", recvWindow)
                    .header("X-BAPI-SIGN", signature)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return extractAllCoins(jsonResponse);

        } catch (Exception e) {
            logger.error("Error getting all coins from bybit: {}", e.getMessage());
            throw new RuntimeException("Error getting all coins: " + e.getMessage(), e);
        }
    }

    private Map<String, Double> extractAllCoins(String jsonResponse) {
        Map<String, Double> result = new HashMap<>();
        try {
            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            int retCode = root.get("retCode").getAsInt();
            if (retCode != 0) throw new RuntimeException("Bybit API error: " + root.get("retMsg").getAsString());

            JsonObject res = root.getAsJsonObject("result");
            JsonArray list = res.getAsJsonArray("list");

            if (list.size() > 0) {
                JsonObject account = list.get(0).getAsJsonObject();
                JsonArray coins = account.getAsJsonArray("coin");
                for (int i = 0; i < coins.size(); i++) {
                    JsonObject coin = coins.get(i).getAsJsonObject();
                    String coinName = coin.get("coin").getAsString();
                    String balStr = coin.get("walletBalance").getAsString();
                    if (balStr != null && !balStr.isEmpty()) {
                        double bal = Double.parseDouble(balStr);
                        if (bal > 0) result.put(coinName, bal);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing all coins from bybit: {}", e.getMessage());
        }
        return result;
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

    public double getWalletBalance(String symbol) { return getBalance(symbol); }
    public double getCoinBalance(String symbol) { return getBalance(symbol); }

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
            for (byte b : bytes) formatter.format("%02x", b);
            return formatter.toString();
        }
    }
}