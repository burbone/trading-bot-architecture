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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

@Service
public class KucoinCancelOrder {
    private static final Logger logger = LoggerFactory.getLogger(KucoinCancelOrder.class);

    private final WebClient webClient;
    private final KucoinConfig kucoinConfig;
    private final BotConfig botConfig;

    @Getter
    private boolean accept = true;

    @Autowired
    public KucoinCancelOrder(WebClient.Builder webClientBuilder, KucoinConfig kucoinConfig, BotConfig botConfig) {
        this.kucoinConfig = kucoinConfig;
        this.botConfig = botConfig;
        this.webClient = webClientBuilder
                .baseUrl(kucoinConfig.getBaseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void cancelOrder(String orderId) {
        if (botConfig.isSimulationMode()) {
            logger.info("[SIMULATION] Kucoin - cancel - {}", orderId);
            return;
        }
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String endpoint = "/api/v1/orders/" + orderId;
        String method = "DELETE";

        String strToSign = timestamp + method + endpoint;
        String signature = generateSignature(strToSign, kucoinConfig.getSecret());
        String passphrase = generatePassphrase(kucoinConfig.getPassphrase(), kucoinConfig.getSecret());

        try {
            String jsonResponse = webClient.delete()
                    .uri(endpoint)
                    .header("KC-API-KEY", kucoinConfig.getKey())
                    .header("KC-API-SIGN", signature)
                    .header("KC-API-TIMESTAMP", timestamp)
                    .header("KC-API-PASSPHRASE", passphrase)
                    .header("KC-API-KEY-VERSION", "3")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            validateResponse(jsonResponse);

            logger.info("Kucoin - cancel - {}", orderId);

        } catch (Exception e) {
            this.accept = false;
            logger.error("Error for cancel order on kucoin {}: {}", orderId, e.getMessage());
            throw new RuntimeException("Error cancelling order " + orderId + ": " + e.getMessage(), e);
        }
    }

    private void validateResponse(String jsonResponse) {
        JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
        String code = root.get("code").getAsString();
        if (!"200000".equals(code)) {
            throw new RuntimeException("KuCoin API error: " + code);
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
            throw new RuntimeException("Ошибка при генерации подписи", e);
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
            throw new RuntimeException("Ошибка при генерации passphrase", e);
        }
    }
}