package com.mySelfCode.algo.api.bybit;

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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Formatter;

@Service
public class BybitCancelOrder {
    private static final Logger logger = LoggerFactory.getLogger(BybitCancelOrder.class);

    private final WebClient webClient;
    private final BybitConfig bybitConfig;
    private final BybitTimeService bybitTimeService;

    @Getter
    private boolean accept = true;

    @Autowired
    public BybitCancelOrder(WebClient.Builder webClientBuilder, BybitConfig bybitConfig, BybitTimeService bybitTimeService) {
        this.bybitConfig = bybitConfig;
        this.bybitTimeService = bybitTimeService;
        this.webClient = webClientBuilder
                .baseUrl(bybitConfig.getBaseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void cancelOrder(String symbol, String orderId) {
        String timestamp = String.valueOf(bybitTimeService.getServerTime());
        String recvWindow = "5000";

        JsonObject orderData = new JsonObject();
        orderData.addProperty("category", "spot");
        orderData.addProperty("symbol", symbol);
        orderData.addProperty("orderId", orderId);

        String requestBody = orderData.toString();
        String signature = generateSignature(timestamp, bybitConfig.getKey(), recvWindow, requestBody);

        try {
            String jsonResponse = webClient.post()
                    .uri("/v5/order/cancel")
                    .header("X-BAPI-API-KEY", bybitConfig.getKey())
                    .header("X-BAPI-TIMESTAMP", timestamp)
                    .header("X-BAPI-RECV-WINDOW", recvWindow)
                    .header("X-BAPI-SIGN", signature)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            validateResponse(jsonResponse);
            logger.info("Bybit - cancel - {} - {}", orderId, symbol);

        } catch (Exception e) {
            this.accept = false;
            logger.error("Error to cancel order on bybit {} for {}: {}", orderId, symbol, e.getMessage());
            throw new RuntimeException("Error cancelling order " + orderId + ": " + e.getMessage(), e);
        }
    }

    private void validateResponse(String jsonResponse) {
        JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
        int retCode = root.get("retCode").getAsInt();
        if (retCode != 0) {
            throw new RuntimeException("Bybit API error: " + root.get("retMsg").getAsString());
        }
    }

    private String generateSignature(String timestamp, String apiKey, String recvWindow, String requestBody) {
        try {
            String payload = timestamp + apiKey + recvWindow + requestBody;
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