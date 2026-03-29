package com.mySelfCode.algo.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class History {
    private String type;
    private double bybitPrice;
    private double kucoinPrice;
}
