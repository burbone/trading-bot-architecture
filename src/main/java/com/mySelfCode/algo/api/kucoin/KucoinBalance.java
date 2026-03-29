package com.mySelfCode.algo.api.kucoin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mySelfCode.algo.cfg.KucoinConfig;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

@Service
public class KucoinBalance {
    private static final Logger logger = LoggerFactory.getLogger(KucoinBalance.class);

    private final WebClient webClient;
    private final KucoinConfig kucoinConfig;

    @Getter
    private boolean accept = true;

    @Autowired
    public KucoinBalance(WebClient.Builder webClientBuilder, KucoinConfig kucoinConfig) {
        this.kucoinConfig = kucoinConfig;
        this.webClient = webClientBuilder
                .baseUrl(kucoinConfig.getBaseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public double getWalletBalance(String symbol) {
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String endpoint = "/api/v1/accounts?currency=" + symbol.toUpperCase() + "&type=trade";
        String method = "GET";

        String strToSign = timestamp + method + endpoint;
        String signature = generateSignature(strToSign, kucoinConfig.getSecret());
        String passphrase = generatePassphrase(kucoinConfig.getPassphrase(), kucoinConfig.getSecret());

        try {
            String jsonResponse = webClient.get()
                    .uri(endpoint)
                    .header("KC-API-KEY", kucoinConfig.getKey())
                    .header("KC-API-SIGN", signature)
                    .header("KC-API-TIMESTAMP", timestamp)
                    .header("KC-API-PASSPHRASE", passphrase)
                    .header("KC-API-KEY-VERSION", "3")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            double balance = extractBalanceFromJson(jsonResponse, symbol.toUpperCase());
            return balance;

        } catch (Exception e) {
            this.accept = false;
            logger.error("Error for take balance on kucoin {}: {}", symbol, e.getMessage());
            throw new RuntimeException("Error getting balance for " + symbol + ": " + e.getMessage(), e);
        }
    }

    public double getCoinBalance(String symbol) {
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String endpoint = "/api/v1/accounts?currency=" + symbol.toUpperCase() + "&type=trade";
        String method = "GET";

        String strToSign = timestamp + method + endpoint;
        String signature = generateSignature(strToSign, kucoinConfig.getSecret());
        String passphrase = generatePassphrase(kucoinConfig.getPassphrase(), kucoinConfig.getSecret());

        try {
            String jsonResponse = webClient.get()
                    .uri(endpoint)
                    .header("KC-API-KEY", kucoinConfig.getKey())
                    .header("KC-API-SIGN", signature)
                    .header("KC-API-TIMESTAMP", timestamp)
                    .header("KC-API-PASSPHRASE", passphrase)
                    .header("KC-API-KEY-VERSION", "3")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            double coinBalance = extractBalanceFromJson(jsonResponse, symbol.toUpperCase());
            return coinBalance;

        } catch (Exception e) {
            this.accept = false;
            logger.error("Error take coin balance on kucoin {}: {}", symbol, e.getMessage());
            throw new RuntimeException("Error getting coin balance for " + symbol + ": " + e.getMessage(), e);
        }
    }

    private double extractBalanceFromJson(String jsonResponse, String targetCurrency) {
        try {
            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            String code = root.get("code").getAsString();

            if (!"200000".equals(code)) {
                throw new RuntimeException("KuCoin API error: " + code);
            }

            JsonArray data = root.getAsJsonArray("data");

            for (int i = 0; i < data.size(); i++) {
                JsonObject account = data.get(i).getAsJsonObject();
                if (account.get("currency").getAsString().equals(targetCurrency)) {
                    return account.get("available").getAsDouble();
                }
            }

            return 0.0;

        } catch (Exception e) {
            throw new RuntimeException("Error parsing KuCoin response: " + e.getMessage());
        }
    }

    private String generateSignature(String strToSign, String secret) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKeySpec);
            byte[] hash = hmac.doFinal(strToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error for generate signature", e);
        }
    }

    private String generatePassphrase(String passphrase, String secret) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKeySpec);
            byte[] hash = hmac.doFinal(passphrase.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error for generate passphrase", e);
        }
    }
}