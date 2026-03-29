package com.mySelfCode.algo.api.kucoin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mySelfCode.algo.cfg.KucoinConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
public class KucoinTradeChecker {
    private static final Logger logger = LoggerFactory.getLogger(KucoinTradeChecker.class);

    private final WebClient webClient;
    private final KucoinConfig kucoinConfig;

    @Autowired
    public KucoinTradeChecker(WebClient.Builder webClientBuilder, KucoinConfig kucoinConfig) {
        this.kucoinConfig = kucoinConfig;
        this.webClient = webClientBuilder
                .baseUrl(kucoinConfig.getBaseUrl())
                .build();
    }

    public String checkTrade(String orderId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String endpoint = "/api/v1/orders/" + orderId;
        String queryParams = "?symbol=MNT-USDT";
        String requestPath = endpoint + queryParams;

        String signature = generateSignature(timestamp, "GET", requestPath, "");
        String encryptedPassphrase = generatePassphrase();

        try {
            String jsonResponse = webClient.get()
                    .uri(requestPath)
                    .header("KC-API-KEY", kucoinConfig.getKey())
                    .header("KC-API-SIGN", signature)
                    .header("KC-API-TIMESTAMP", timestamp)
                    .header("KC-API-PASSPHRASE", encryptedPassphrase)
                    .header("KC-API-KEY-VERSION", "3")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();

            if (!root.has("code")) {
                logger.error("Kucoin - order - {}: Missing 'code' in response", orderId);
                return "Error";
            }

            String code = root.get("code").getAsString();

            if (!"200000".equals(code)) {
                logger.error("Kucoin - order - {}: Error code {}", orderId, code);
                return "Error";
            }

            if (!root.has("data") || root.get("data").isJsonNull()) {
                logger.error("Kucoin - order - {}: Missing or null 'data' in response", orderId);
                return "Error";
            }

            JsonObject data = root.getAsJsonObject("data");

            if (!data.has("isActive") || !data.has("cancelExist")) {
                logger.error("Kucoin - order - {}: Missing 'isActive' or 'cancelExist' in data", orderId);
                return "Error";
            }

            boolean isActive = data.get("isActive").getAsBoolean();
            boolean cancelExist = data.get("cancelExist").getAsBoolean();

            if (!isActive && !cancelExist) {
                logger.info("Kucoin - order - {}: Done", orderId);
                return "Done";
            } else if (!isActive && cancelExist) {
                logger.info("Kucoin - order - {}: Error (cancelled)", orderId);
                return "Error";
            } else {
                logger.info("Kucoin - order - {}: InTrade", orderId);
                return "InTrade";
            }

        } catch (Exception e) {
            logger.error("Error for check trade on kucoin {}: {}", orderId, e.getMessage());
            return "Error";
        }
    }

    private String generateSignature(String timestamp, String method, String requestPath, String body) {
        try {
            String strToSign = timestamp + method + requestPath + body;
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    kucoinConfig.getSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            hmac.init(secretKeySpec);
            byte[] hash = hmac.doFinal(strToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Ошибка генерации подписи", e);
        }
    }

    private String generatePassphrase() {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    kucoinConfig.getSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            hmac.init(secretKeySpec);
            byte[] hash = hmac.doFinal(kucoinConfig.getPassphrase().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Ошибка генерации passphrase", e);
        }
    }
}