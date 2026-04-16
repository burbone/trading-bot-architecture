package com.mySelfCode.algo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MinTradeInfo {
    private double minCryptoBybit;
    private double minUsdtBybit;
    private int cryptoPrecisionBybit;
    private int usdtPrecisionBybit;

    private double minCryptoKucoin;
    private double minUsdtKucoin;
    private int cryptoPrecisionKucoin;
    private int usdtPrecisionKucoin;
}