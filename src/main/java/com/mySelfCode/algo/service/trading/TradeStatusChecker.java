package com.mySelfCode.algo.service.trading;

import com.mySelfCode.algo.api.bybit.BybitTradeChecker;
import com.mySelfCode.algo.api.kucoin.KucoinTradeChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradeStatusChecker {

    private final BybitTradeChecker bybitChecker;
    private final KucoinTradeChecker kucoinChecker;

    public String checkBybit(String orderId) {
        return bybitChecker.checkTrade(orderId);
    }

    public String checkKucoin(String orderId) {
        return kucoinChecker.checkTrade(orderId);
    }
}