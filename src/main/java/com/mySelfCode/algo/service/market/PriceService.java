package com.mySelfCode.algo.service.market;

import com.mySelfCode.algo.api.bybit.BybitCheckPrice;
import com.mySelfCode.algo.api.kucoin.KucoinCheckPrice;
import com.mySelfCode.algo.dto.TradingPair;
import com.mySelfCode.algo.dto.TradingPairRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceService {

    private final TradingPairRegistry registry;
    private final BybitCheckPrice bybitCheckPrice;
    private final KucoinCheckPrice kucoinCheckPrice;

    private final ExecutorService executor = Executors.newFixedThreadPool(8);

    public CompletableFuture<Void> updateAllPricesAsync() {
        List<TradingPair> pairs = registry.getPairs();
        if (pairs.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<?>[] futures = pairs.stream()
                .map(pair -> CompletableFuture.runAsync(() -> updatePairPrices(pair), executor))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures);
    }

    private void updatePairPrices(TradingPair pair) {
        try {
            double[] bybit = bybitCheckPrice.bybitCheckPrice(pair.getBybitSymbol());
            pair.setBybitBidPrice(bybit[0]);
            pair.setBybitAskPrice(bybit[1]);

            double[] kucoin = kucoinCheckPrice.kucoinCheckPrice(pair.getKucoinSymbol());
            pair.setKucoinBidPrice(kucoin[0]);
            pair.setKucoinAskPrice(kucoin[1]);
        } catch (Exception e) {
            log.error("[{}] price update failed: {}", pair.getSymbol(), e.getMessage());
        }
    }
}