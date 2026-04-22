package com.mySelfCode.algo.service.analytics;

import com.mySelfCode.algo.dto.CompletedTrade;
import com.mySelfCode.algo.dto.ExchangeUsdt;
import com.mySelfCode.algo.dto.TradingPair;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfitCalculator {

    private final ExchangeUsdt exchangeUsdt;

    public CompletedTrade calculate(TradingPair pair, double entryBybitPrice, double entryKucoinPrice,
                                    double exitBybitPrice, double exitKucoinPrice,
                                    double totalBalanceBefore) {
        CompletedTrade trade = new CompletedTrade();
        trade.setTimestamp(System.currentTimeMillis());
        trade.setSymbol(pair.getSymbol());
        trade.setBybitEntryPrice(entryBybitPrice);
        trade.setBybitExitPrice(exitBybitPrice);
        trade.setKucoinEntryPrice(entryKucoinPrice);
        trade.setKucoinExitPrice(exitKucoinPrice);

        double currentTotalBalance = exchangeUsdt.getBybitUsdt() + exchangeUsdt.getKucoinUsdt() +
                pair.getBybitCrypto() + pair.getKucoinCrypto();
        double totalProfitUsd = currentTotalBalance - totalBalanceBefore;
        double totalProfitPercent = totalBalanceBefore > 0 ? (totalProfitUsd / totalBalanceBefore) * 100 : 0;

        double priceGrowthProfit = calculatePriceGrowthProfit(pair, entryBybitPrice, entryKucoinPrice,
                exitBybitPrice, exitKucoinPrice);
        double profitExcludingPriceGrowthUsd = totalProfitUsd - priceGrowthProfit;
        double profitExcludingPriceGrowthPercent = totalBalanceBefore > 0 ?
                (profitExcludingPriceGrowthUsd / totalBalanceBefore) * 100 : 0;

        trade.setTotalProfitUsd(totalProfitUsd);
        trade.setTotalProfitPercent(totalProfitPercent);
        trade.setProfitExcludingPriceGrowthUsd(profitExcludingPriceGrowthUsd);
        trade.setProfitExcludingPriceGrowthPercent(profitExcludingPriceGrowthPercent);

        return trade;
    }

    private double calculatePriceGrowthProfit(TradingPair pair, double entryBybit, double entryKucoin,
                                              double exitBybit, double exitKucoin) {
        double bybitGrowth = (exitBybit - entryBybit) * pair.getBybitCoins();
        double kucoinGrowth = (exitKucoin - entryKucoin) * pair.getKucoinCoins();
        return bybitGrowth + kucoinGrowth;
    }
}