package com.mySelfCode.algo.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class MinMountTrade {
    private double minUsdtBybit;
    private double minCryptoBybit;
    private double minUsdtKucoin;
    private double minCryptoKucoin;

    private int minUsdtPrecisionBybit;
    private int minCyptoPrecisionBybit;
    private int minUsdtPrecisionKucoin;
    private int minCyptoPrecisionKucoin;
}
