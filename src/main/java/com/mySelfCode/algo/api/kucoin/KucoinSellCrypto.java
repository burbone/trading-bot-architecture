package com.mySelfCode.algo.api.kucoin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mySelfCode.algo.cfg.BotConfig;
import com.mySelfCode.algo.cfg.KucoinConfig;
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
    private final BotConfig botConfig;

    @Getter
    private boolean accept = true;

    @Autowired
    public KucoinSellCrypto(WebClient.Builder webClientBuilder, KucoinConfig kucoinConfig, BotConfig botConfig) {
        this.kucoinConfig = kucoinConfig;
        this.botConfig = botConfig;
        this.webClient = webClientBuilder
                .baseUrl(kucoinConfig.getBaseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void sellMarket(TradingPair pair, double amount, int cryptoPrecision) {
        String symbol = pair.getKucoinSymbol();
        if (botConfig.isSimulationMode()) {
            String fakeOrderId = "SIM-KUCOIN-SELL-" + UUID.randomUUID().toString().substring(0, 8);
            pair.setKucoinSellOrderId(fakeOrderId);
            logger.info("[SIMULATION] Kucoin sell {} | {}$ | orderId={}", symbol, amount, fakeOrderId);
            return;
        }
        double bidPrice = pair.getKucoinBidPrice();
        if (bidPrice <= 0) {
            throw new RuntimeException("Kucoin bid price is 0 for " + symbol + ", cannot calculate coin quantity");
        }
        double coinQuantity = amount / bidPrice;
        coinQuantity = coinQuantity * (1 - botConfig.getKucoinFee() * 5);
        BigDecimal cQ = BigDecimal.valueOf(coinQuantity).setScale(cryptoPrecision, RoundingMode.FLOOR);
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

            validateResponse(jsonResponse);

            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            JsonElement dataElement = root.get("data");
            String orderId = dataElement.isJsonObject()
                    ? dataElement.getAsJsonObject().get("orderId").getAsString()
                    : dataElement.getAsString();
            pair.setKucoinSellOrderId(orderId);
            logger.info("Kucoin sell {} | {}$ (~{} coins) | orderId={}", symbol, amount, cQ.toPlainString(), orderId);

        } catch (Exception e) {
            this.accept = false;
            logger.error("Kucoin sell error {} amount={}: {}", symbol, amount, e.getMessage());
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
            return Base64.getEncoder().encodeToString(hmac.doFinal(strToSign.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Ошибка при генерации подписи", e);
        }
    }

    private String generatePassphrase(String passphrase, String secret) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKeySpec);
            return Base64.getEncoder().encodeToString(hmac.doFinal(passphrase.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Ошибка при генерации passphrase", e);
        }
    }
}