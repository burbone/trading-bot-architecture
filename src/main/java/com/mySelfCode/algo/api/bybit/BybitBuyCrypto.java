package com.mySelfCode.algo.api.bybit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mySelfCode.algo.cfg.BotConfig;
import com.mySelfCode.algo.cfg.BybitConfig;
import com.mySelfCode.algo.dto.LastOrderIds;
import com.mySelfCode.algo.dto.MinMountTrade;
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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Formatter;

@Service
public class BybitBuyCrypto {
    private static final Logger logger = LoggerFactory.getLogger(BybitBuyCrypto.class);

    private final WebClient webClient;
    private final BybitConfig bybitConfig;
    private final StarterInfo starterInfo;
    private final LastOrderIds lastOrderIds;
    private final MinMountTrade minMountTrade;
    private final BotConfig botConfig;

    @Getter
    private boolean accept = true;

    @Autowired
    public BybitBuyCrypto(WebClient.Builder webClientBuilder, BybitConfig bybitConfig, StarterInfo starterInfo, LastOrderIds lastOrderIds, MinMountTrade minMountTrade, BotConfig botConfig) {
        this.bybitConfig = bybitConfig;
        this.starterInfo = starterInfo;
        this.lastOrderIds = lastOrderIds;
        this.minMountTrade = minMountTrade;
        this.botConfig = botConfig;
        this.webClient = webClientBuilder
                .baseUrl(bybitConfig.getBaseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void buyMarket(Double amount) {
        amount = amount * (1 - botConfig.getBybitFee() * 5);
        BigDecimal amountBig = BigDecimal.valueOf(amount)
                .setScale(minMountTrade.getMinUsdtPrecisionBybit(), RoundingMode.FLOOR);
        String symbol = starterInfo.getSymbol().replaceAll(" ", "");
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String recvWindow = "5000";

        JsonObject orderData = new JsonObject();
        orderData.addProperty("category", "spot");
        orderData.addProperty("symbol", symbol);
        orderData.addProperty("side", "Buy");
        orderData.addProperty("orderType", "Market");
        orderData.addProperty("marketUnit", "quoteCoin");
        orderData.addProperty("qty", amountBig.toPlainString());
        orderData.addProperty("timeInForce", "IOC");
        orderData.addProperty("isLeverage", 0);
        orderData.addProperty("orderFilter", "Order");

        String requestBody = orderData.toString();
        String signature = generateSignature(timestamp, bybitConfig.getKey(), recvWindow, requestBody);

        try {
            String jsonResponse = webClient.post()
                    .uri("/v5/order/create")
                    .header("X-BAPI-API-KEY", bybitConfig.getKey())
                    .header("X-BAPI-TIMESTAMP", timestamp)
                    .header("X-BAPI-RECV-WINDOW", recvWindow)
                    .header("X-BAPI-SIGN", signature)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            validateResponse(jsonResponse);

            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            JsonObject result = root.getAsJsonObject("result");
            String orderId = result.get("orderId").getAsString();
            lastOrderIds.setBybitBuyOrderId(orderId);

            logger.info("Bybit - buy - {} - {} - USDT", symbol, amount);

        } catch (Exception e) {
            this.accept = false;
            logger.error("Error for buy on bybit {} for mount {}: {}", symbol, amount, e.getMessage());
            throw new RuntimeException("Error buying " + symbol + ": " + e.getMessage(), e);
        }
    }

    private void validateResponse(String jsonResponse) {
        JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
        int retCode = root.get("retCode").getAsInt();

        if (retCode != 0) {
            String retMsg = root.get("retMsg").getAsString();
            throw new RuntimeException("Bybit API error: " + retMsg);
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
            for (byte b : bytes) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }
}