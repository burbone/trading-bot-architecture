package com.mySelfCode.algo.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class BybitData {
    private volatile double askPrice;
    private volatile double bidPrice;
}
