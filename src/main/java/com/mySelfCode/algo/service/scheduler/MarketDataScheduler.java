package com.mySelfCode.algo.service.scheduler;

import com.mySelfCode.algo.dto.Status;
import com.mySelfCode.algo.service.market.BalanceService;
import com.mySelfCode.algo.service.market.PriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataScheduler {

    private final Status status;
    private final PriceService priceService;
    private final BalanceService balanceService;

    @Scheduled(fixedRate = 1000)
    public void updateMarketData() {
        if (!status.isStatus()) return;

        priceService.updateAllPrices();
        balanceService.updateAllBalances();
    }
}