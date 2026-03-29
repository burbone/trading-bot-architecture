package com.mySelfCode.algo.service;

import com.mySelfCode.algo.api.bybit.BybitBalance;
import com.mySelfCode.algo.api.bybit.BybitCheckPrice;
import com.mySelfCode.algo.api.kucoin.KucoinBalance;
import com.mySelfCode.algo.api.kucoin.KucoinCheckPrice;
import com.mySelfCode.algo.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataParser {
    private final Status status;
    private final StarterInfo starterInfo;
    private final Profile profile;
    private final BybitData bybitData;
    private final KucoinData kucoinData;

    private final BybitBalance bybitBalance;
    private final KucoinBalance kucoinBalance;
    private final BybitCheckPrice bybitCheckPrice;
    private final KucoinCheckPrice kucoinCheckPrice;

    private final String usdt = "USDT";
    private final ExecutorService executor = Executors.newFixedThreadPool(16);

    @Scheduled(fixedRate = 1000)
    public void StartDataParser() {
        if (status.isStatus()) {
            String symbol = starterInfo.getSymbol().replaceAll(" ", "").replaceAll(usdt, "");
            try {
                CompletableFuture<Double> bybitUsdtFuture = CompletableFuture.supplyAsync(() ->
                        bybitBalance.getWalletBalance(usdt), executor
                );
                CompletableFuture<Double> bybitCoinsFuture = CompletableFuture.supplyAsync(() ->
                        bybitBalance.getCoinBalance(symbol), executor
                );
                CompletableFuture<Double> kucoinUsdtFuture = CompletableFuture.supplyAsync(() ->
                        kucoinBalance.getWalletBalance(usdt), executor
                );
                CompletableFuture<Double> kucoinCoinsFuture = CompletableFuture.supplyAsync(() ->
                        kucoinBalance.getCoinBalance(symbol), executor
                );

                CompletableFuture.allOf(bybitUsdtFuture, bybitCoinsFuture, kucoinUsdtFuture, kucoinCoinsFuture).join();

                double bybitUsdt = bybitUsdtFuture.get();
                double bybitCoins = bybitCoinsFuture.get();
                double kucoinUsdt = kucoinUsdtFuture.get();
                double kucoinCoins = kucoinCoinsFuture.get();

                profile.setBybitUsdt(bybitUsdt);
                profile.setBybitCoins(bybitCoins);
                profile.setKucoinUsdt(kucoinUsdt);
                profile.setKucoinCoins(kucoinCoins);

                double bybitMidPrice = (bybitData.getAskPrice() + bybitData.getBidPrice()) / 2;
                double kucoinMidPrice = (kucoinData.getAskPrice() + kucoinData.getBidPrice()) / 2;

                profile.setBybitCrypto(bybitCoins * bybitMidPrice);
                profile.setKucoinCrypto(kucoinCoins * kucoinMidPrice);

            } catch (Exception e) {
                log.error("Error data parser: {}", e.getMessage());
            }
        }
    }

    @Scheduled(fixedRate = 1000)
    public void StartPricesParser() {
        if (status.isStatus()) {
            try {
                CompletableFuture<double[]> bybitPricesFuture = CompletableFuture.supplyAsync(() ->
                        bybitCheckPrice.bybitCheckPrice(), executor
                );
                CompletableFuture<double[]> kucoinPricesFuture = CompletableFuture.supplyAsync(() ->
                        kucoinCheckPrice.kucoinCheckPrice(), executor
                );

                CompletableFuture.allOf(bybitPricesFuture, kucoinPricesFuture).join();

                double[] bybitPrices = bybitPricesFuture.get();
                double[] kucoinPrices = kucoinPricesFuture.get();

                bybitData.setAskPrice(bybitPrices[1]);
                bybitData.setBidPrice(bybitPrices[0]);

                kucoinData.setAskPrice(kucoinPrices[1]);
                kucoinData.setBidPrice(kucoinPrices[0]);

            } catch (Exception e) {
                log.error("Error price parser: {}", e.getMessage());
            }
        }
    }
}