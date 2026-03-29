package com.mySelfCode.algo.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class LastOrderIds {
    private volatile String bybitBuyOrderId;
    private volatile String bybitSellOrderId;
    private volatile String kucoinBuyOrderId;
    private volatile String kucoinSellOrderId;
}