package com.mySelfCode.algo.service.market;

import com.mySelfCode.algo.api.bybit.BybitBalance;
import com.mySelfCode.algo.api.kucoin.KucoinBalance;
import com.mySelfCode.algo.dto.ExchangeUsdt;
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
public class BalanceService {

    private final TradingPairRegistry registry;
    private final ExchangeUsdt exchangeUsdt;
    private final BybitBalance bybitBalance;
    private final KucoinBalance kucoinBalance;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public CompletableFuture<Void> updateAllBalancesAsync() {
        List<TradingPair> pairs = registry.getPairs();

        CompletableFuture<Void> usdtFuture = CompletableFuture.runAsync(() -> {
            exchangeUsdt.setBybitUsdt(bybitBalance.getBalance("USDT"));
            exchangeUsdt.setKucoinUsdt(kucoinBalance.getBalance("USDT"));
        }, executor);

        CompletableFuture<?>[] coinFutures = pairs.stream()
                .map(pair -> CompletableFuture.runAsync(() -> updatePairBalances(pair), executor))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(
                CompletableFuture.allOf(coinFutures),
                usdtFuture
        );
    }

    private void updatePairBalances(TradingPair pair) {
        try {
            double bybitCoins = bybitBalance.getBalance(pair.getCoinSymbol());
            double bybitMid = (pair.getBybitAskPrice() + pair.getBybitBidPrice()) / 2;
            pair.setBybitCoins(bybitCoins);
            pair.setBybitCrypto(bybitMid > 0 ? bybitCoins * bybitMid : 0);

            double kucoinCoins = kucoinBalance.getBalance(pair.getCoinSymbol());
            double kucoinMid = (pair.getKucoinAskPrice() + pair.getKucoinBidPrice()) / 2;
            pair.setKucoinCoins(kucoinCoins);
            pair.setKucoinCrypto(kucoinMid > 0 ? kucoinCoins * kucoinMid : 0);
        } catch (Exception e) {
            log.error("[{}] balance update failed: {}", pair.getSymbol(), e.getMessage());
        }
    }
}