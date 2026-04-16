package com.mySelfCode.algo.service.trading;

import com.mySelfCode.algo.api.bybit.BybitBuyCrypto;
import com.mySelfCode.algo.api.bybit.BybitSellCrypto;
import com.mySelfCode.algo.api.kucoin.KucoinBuyCrypto;
import com.mySelfCode.algo.api.kucoin.KucoinSellCrypto;
import com.mySelfCode.algo.dto.TradingPair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExecutor {

    private final BybitBuyCrypto bybitBuy;
    private final BybitSellCrypto bybitSell;
    private final KucoinBuyCrypto kucoinBuy;
    private final KucoinSellCrypto kucoinSell;

    public void bybitBuy(TradingPair pair, double usdtAmount, int usdtPrecision) {
        bybitBuy.buyMarket(pair, usdtAmount, usdtPrecision);
    }

    public void bybitSell(TradingPair pair, double usdtAmount, int cryptoPrecision) {
        bybitSell.sellMarket(pair, usdtAmount, cryptoPrecision);
    }

    public void kucoinBuy(TradingPair pair, double usdtAmount, int usdtPrecision) {
        kucoinBuy.buyMarket(pair, usdtAmount, usdtPrecision);
    }

    public void kucoinSell(TradingPair pair, double usdtAmount, int cryptoPrecision) {
        kucoinSell.sellMarket(pair, usdtAmount, cryptoPrecision);
    }
}