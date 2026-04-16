package com.mySelfCode.algo.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class ExchangeUsdt {
    private volatile double bybitUsdt;
    private volatile double kucoinUsdt;
}