package com.mySelfCode.algo.api.kucoin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mySelfCode.algo.cfg.BotConfig;
import com.mySelfCode.algo.cfg.KucoinConfig;
import com.mySelfCode.algo.dto.LastOrderIds;
import com.mySelfCode.algo.dto.MinMountTrade;
import com.mySelfCode.algo.dto.Profile;
import com.mySelfCode.algo.dto.StarterInfo;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class KucoinSellCrypto {
    private static final Logger logger = LoggerFactory.getLogger(KucoinSellCrypto.class);

    private final WebClient webClient;
    private final KucoinConfig kucoinConfig;
    private final StarterInfo starterInfo;
    private final Profile profile;
    private final LastOrderIds lastOrderIds;
    private final MinMountTrade minMountTrade;
    private final KucoinCheckPrice kucoinCheckPrice;
    private final BotConfig botConfig;


    @Getter
    private boolean accept = true;

    @Autowired
    public KucoinSellCrypto(WebClient.Builder webClientBuilder, KucoinConfig kucoinConfig, StarterInfo starterInfo, Profile profile, LastOrderIds lastOrderIds, MinMountTrade minMountTrade, KucoinCheckPrice kucoinCheckPrice, BotConfig botConfig) {
        this.profile = profile;
        this.kucoinConfig = kucoinConfig;
        this.starterInfo = starterInfo;
        this.lastOrderIds = lastOrderIds;
        this.minMountTrade = minMountTrade;
        this.kucoinCheckPrice = kucoinCheckPrice;
        this.botConfig = botConfig;
        this.webClient = webClientBuilder
                .baseUrl(kucoinConfig.getBaseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void sellMarket(Double amount) {
        String symbol = starterInfo.getSymbol().replaceAll(" ", "-");
        double[] prices = kucoinCheckPrice.kucoinCheckPrice();
        double bidPrice = prices[0];
        double coinQuantity = amount / bidPrice;
        coinQuantity = coinQuantity * (1 - botConfig.getKucoinFee() * 5);

        BigDecimal cQ = BigDecimal.valueOf(coinQuantity)
                .setScale(minMountTrade.getMinCyptoPrecisionKucoin(), RoundingMode.FLOOR);

        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String endpoint = "/api/v1/orders";
        String method = "POST";

        JsonObject orderData = new JsonObject();
        orderData.addProperty("type", "market");
        orderData.addProperty("symbol", symbol);
        orderData.addProperty("side", "sell");
        orderData.addProperty("size", cQ.toPlainString());
        orderData.addProperty("clientOid", UUID.randomUUID().toString());

        String requestBody = orderData.toString();
        String strToSign = timestamp + method + endpoint + requestBody;
        String signature = generateSignature(strToSign, kucoinConfig.getSecret());
        String passphrase = generatePassphrase(kucoinConfig.getPassphrase(), kucoinConfig.getSecret());

        try {
            String jsonResponse = webClient.post()
                    .uri(endpoint)
                    .header("KC-API-KEY", kucoinConfig.getKey())
                    .header("KC-API-SIGN", signature)
                    .header("KC-API-TIMESTAMP", timestamp)
                    .header("KC-API-PASSPHRASE", passphrase)
                    .header("KC-API-KEY-VERSION", "3")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            logger.info("Kucoin - sell - response: {}", jsonResponse);
            validateResponse(jsonResponse);

            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            JsonElement dataElement = root.get("data");
            String orderId;
            if (dataElement.isJsonObject()) {
                orderId = dataElement.getAsJsonObject().get("orderId").getAsString();
            } else {
                orderId = dataElement.getAsString();
            }
            lastOrderIds.setKucoinSellOrderId(orderId);
            logger.info("Kucoin - sell - {} - {} - orderId: {}", symbol, amount, orderId);

        } catch (Exception e) {
            this.accept = false;
            logger.error("Error while sell on kucoin {} for amount {}: {}", symbol, amount, e.getMessage());
            throw new RuntimeException("Error selling " + symbol + ": " + e.getMessage(), e);
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