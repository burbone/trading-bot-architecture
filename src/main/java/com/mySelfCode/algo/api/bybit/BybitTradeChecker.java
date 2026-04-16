package com.mySelfCode.algo.api.bybit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mySelfCode.algo.cfg.BotConfig;
import com.mySelfCode.algo.cfg.BybitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Formatter;

@Service
public class BybitTradeChecker {
    private static final Logger logger = LoggerFactory.getLogger(BybitTradeChecker.class);

    private final WebClient webClient;
    private final BybitConfig bybitConfig;
    private final BotConfig botConfig;
    private final BybitTimeService bybitTimeService;

    @Autowired
    public BybitTradeChecker(WebClient.Builder webClientBuilder, BybitConfig bybitConfig, BotConfig botConfig, BybitTimeService bybitTimeService) {
        this.bybitConfig = bybitConfig;
        this.botConfig = botConfig;
        this.bybitTimeService = bybitTimeService;
        this.webClient = webClientBuilder
                .baseUrl(bybitConfig.getBaseUrl())
                .build();
    }

    public String checkTrade(String orderId) {
        if (botConfig.isSimulationMode()) {
            logger.info("[SIMULATION] Bybit - order check - {}: Done", orderId);
            return "Done";
        }

        String timestamp = String.valueOf(bybitTimeService.getServerTime());
        String recvWindow = "5000";
        String queryString = "category=spot&orderId=" + orderId;
        String payload = timestamp + bybitConfig.getKey() + recvWindow + queryString;
        String signature = generateSignature(payload);

        try {
            String jsonResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v5/execution/list")
                            .queryParam("category", "spot")
                            .queryParam("orderId", orderId)
                            .build())
                    .header("X-BAPI-API-KEY", bybitConfig.getKey())
                    .header("X-BAPI-TIMESTAMP", timestamp)
                    .header("X-BAPI-RECV-WINDOW", recvWindow)
                    .header("X-BAPI-SIGN", signature)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            int retCode = root.get("retCode").getAsInt();

            if (retCode != 0) return "NotFound";

            JsonObject result = root.getAsJsonObject("result");
            JsonArray list = result.getAsJsonArray("list");

            if (list == null || list.size() == 0) {
                logger.info("Bybit - order - {}: InTrade", orderId);
                return "InTrade";
            }

            logger.info("Bybit - order - {}: Done", orderId);
            return "Done";

        } catch (Exception e) {
            logger.error("Error for check trade on bybit {}: {}", orderId, e.getMessage());
            return "Error";
        }
    }

    private String generateSignature(String payload) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(bybitConfig.getSecret().getBytes(), "HmacSHA256");
            hmac.init(secretKeySpec);
            byte[] hash = hmac.doFinal(payload.getBytes());
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Ошибка генерации подписи", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        try (Formatter formatter = new Formatter()) {
            for (byte b : bytes) formatter.format("%02x", b);
            return formatter.toString();
        }
    }
}