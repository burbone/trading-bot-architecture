package com.mySelfCode.algo.service.trading;

import com.mySelfCode.algo.dto.MinTradeInfo;
import com.mySelfCode.algo.dto.TradingPair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PairTradeService {

    private final OrderExecutor orderExecutor;

    public void sellBybitBuyKucoin(TradingPair pair, double sellAmountUsdt, double buyAmountUsdt,
                                   MinTradeInfo info) {
        try {
            orderExecutor.bybitSell(pair, sellAmountUsdt, info.getCryptoPrecisionBybit());
            orderExecutor.kucoinBuy(pair, buyAmountUsdt, info.getUsdtPrecisionKucoin());
            log.info("[{}] entry executed: sell Bybit {} USD, buy Kucoin {} USD",
                    pair.getSymbol(), sellAmountUsdt, buyAmountUsdt);
        } catch (Exception e) {
            log.error("[{}] entry failed: {}", pair.getSymbol(), e.getMessage());
            pair.setProcess("ERROR");
        }
    }

    public void buyBybitSellKucoin(TradingPair pair, double buyAmountUsdt, double sellAmountUsdt,
                                   MinTradeInfo info) {
        try {
            orderExecutor.bybitBuy(pair, buyAmountUsdt, info.getUsdtPrecisionBybit());
            orderExecutor.kucoinSell(pair, sellAmountUsdt, info.getCryptoPrecisionKucoin());
            log.info("[{}] entry executed: buy Bybit {} USD, sell Kucoin {} USD",
                    pair.getSymbol(), buyAmountUsdt, sellAmountUsdt);
        } catch (Exception e) {
            log.error("[{}] entry failed: {}", pair.getSymbol(), e.getMessage());
            pair.setProcess("ERROR");
        }
    }
}