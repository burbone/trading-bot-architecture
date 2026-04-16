package com.mySelfCode.algo.service.algorithm;

import com.mySelfCode.algo.dto.ExchangeUsdt;
import com.mySelfCode.algo.dto.MinTradeInfo;
import com.mySelfCode.algo.dto.TradingPair;
import com.mySelfCode.algo.dto.TradingPairRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
@RequiredArgsConstructor
public class EntryConditionEvaluator {

    private final TradingPairRegistry registry;
    private final ExchangeUsdt exchangeUsdt;

    public record EntryOpportunity(boolean valid, String direction,
                                   double sellAmountUsdt, double buyAmountUsdt,
                                   double sellCoins) {

        public static EntryOpportunity none() {
            return new EntryOpportunity(false, null, 0, 0, 0);
        }

        public boolean isBybitToKucoin() {
            return "BYBIT_TO_KUCOIN".equals(direction);
        }

        public boolean isKucoinToBybit() {
            return "KUCOIN_TO_BYBIT".equals(direction);
        }
    }

    public EntryOpportunity evaluate(TradingPair pair, MinTradeInfo info) {
        double threshold = 1 + (pair.getPercent() / 100.0);

        if (pair.getBybitBidPrice() >= pair.getKucoinAskPrice() * threshold) {
            if (registry.isLocked()) return EntryOpportunity.none();

            double sellAmountUsdt = pair.getBybitCrypto();
            double buyAmountUsdt = Math.min(exchangeUsdt.getKucoinUsdt(), sellAmountUsdt);
            double sellCoins = pair.getBybitBidPrice() > 0 ? sellAmountUsdt / pair.getBybitBidPrice() : 0;

            if (!checkMinSellSize(sellAmountUsdt, pair.getBybitBidPrice(), info.getMinCryptoBybit(), info.getCryptoPrecisionBybit())
                    || buyAmountUsdt < info.getMinUsdtKucoin()) {
                log.debug("[{}] entry skipped (min size): sellCoins insufficient or buyUsdt < min", pair.getSymbol());
                return EntryOpportunity.none();
            }

            return new EntryOpportunity(true, "BYBIT_TO_KUCOIN",
                    sellAmountUsdt, buyAmountUsdt, sellCoins);
        }

        if (pair.getKucoinBidPrice() >= pair.getBybitAskPrice() * threshold) {
            if (registry.isLocked()) return EntryOpportunity.none();

            double sellAmountUsdt = pair.getKucoinCrypto();
            double buyAmountUsdt = Math.min(exchangeUsdt.getBybitUsdt(), sellAmountUsdt);
            double sellCoins = pair.getKucoinBidPrice() > 0 ? sellAmountUsdt / pair.getKucoinBidPrice() : 0;

            if (!checkMinSellSize(sellAmountUsdt, pair.getKucoinBidPrice(), info.getMinCryptoKucoin(), info.getCryptoPrecisionKucoin())
                    || buyAmountUsdt < info.getMinUsdtBybit()) {
                log.debug("[{}] entry skipped (min size): sellCoins insufficient or buyUsdt < min", pair.getSymbol());
                return EntryOpportunity.none();
            }

            return new EntryOpportunity(true, "KUCOIN_TO_BYBIT",
                    sellAmountUsdt, buyAmountUsdt, sellCoins);
        }

        return EntryOpportunity.none();
    }

    private boolean checkMinSellSize(double sellAmountUsdt, double price, double minCrypto, int cryptoPrecision) {
        if (price <= 0) return false;
        BigDecimal sellCoins = BigDecimal.valueOf(sellAmountUsdt)
                .divide(BigDecimal.valueOf(price), cryptoPrecision, RoundingMode.FLOOR);
        return sellCoins.compareTo(BigDecimal.valueOf(minCrypto)) >= 0;
    }
}