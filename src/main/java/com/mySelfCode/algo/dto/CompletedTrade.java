package com.mySelfCode.algo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompletedTrade {
    private long timestamp;
    private String symbol;
    private double totalProfitUsd;
    private double totalProfitPercent;
    private double profitExcludingPriceGrowthUsd;
    private double profitExcludingPriceGrowthPercent;
    private double bybitEntryPrice;
    private double bybitExitPrice;
    private double kucoinEntryPrice;
    private double kucoinExitPrice;
}