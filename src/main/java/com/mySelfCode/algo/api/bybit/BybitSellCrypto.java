package com.mySelfCode.algo.api.bybit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mySelfCode.algo.cfg.BotConfig;
import com.mySelfCode.algo.cfg.BybitConfig;
import com.mySelfCode.algo.dto.TradingPair;
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
import java.util.UUID;

@Service
public class BybitSellCrypto {
    private static final Logger logger = LoggerFactory.getLogger(BybitSellCrypto.class);

    private final WebClient webClient;
    private final BybitConfig bybitConfig;
    private final BotConfig botConfig;
    private final BybitTimeService bybitTimeService;

    @Getter
    private boolean accept = true;

    @Autowired
    public BybitSellCrypto(WebClient.Builder webClientBuilder, BybitConfig bybitConfig, BotConfig botConfig, BybitTimeService bybitTimeService) {
        this.bybitConfig = bybitConfig;
        this.botConfig = botConfig;
        this.bybitTimeService = bybitTimeService;
        this.webClient = webClientBuilder
                .baseUrl(bybitConfig.getBaseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void sellMarket(TradingPair pair, double amount, int cryptoPrecision) {
        String symbol = pair.getBybitSymbol();

        if (botConfig.isSimulationMode()) {
            String fakeOrderId = "SIM-BYBIT-SELL-" + UUID.randomUUID().toString().substring(0, 8);
            pair.setBybitSellOrderId(fakeOrderId);
            logger.info("[SIMULATION] Bybit sell {} | {}$ | orderId={}", symbol, amount, fakeOrderId);
            return;
        }

        double bidPrice = pair.getBybitBidPrice();
        if (bidPrice <= 0) {
            throw new RuntimeException("Bybit bid price is 0 for " + symbol + ", cannot calculate coin quantity");
        }

        double coinQuantity = amount / bidPrice;
        coinQuantity = coinQuantity * (1 - botConfig.getBybitFee() * 5);

        BigDecimal cQ = BigDecimal.valueOf(coinQuantity).setScale(cryptoPrecision, RoundingMode.FLOOR);
        String timestamp = String.valueOf(bybitTimeService.getServerTime());
        String recvWindow = "5000";

        JsonObject orderData = new JsonObject();
        orderData.addProperty("category", "spot");
        orderData.addProperty("symbol", symbol);
        orderData.addProperty("side", "Sell");
        orderData.addProperty("orderType", "Market");
        orderData.addProperty("qty", cQ.toPlainString());
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
            pair.setBybitSellOrderId(orderId);
            logger.info("Bybit sell {} | {}$ (~{} coins) | orderId={}", symbol, amount, cQ.toPlainString(), orderId);

        } catch (Exception e) {
            this.accept = false;
            logger.error("Bybit sell error {} amount={}: {}", symbol, amount, e.getMessage());
            throw new RuntimeException("Error selling " + symbol + ": " + e.getMessage(), e);
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