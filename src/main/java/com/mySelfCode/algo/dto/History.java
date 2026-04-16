package com.mySelfCode.algo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class History {
    private String type;
    private String symbol;
    private double bybitPrice;
    private double kucoinPrice;
}