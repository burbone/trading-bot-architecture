package com.mySelfCode.algo.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class Profile {
    private volatile double bybitUsdt; // $ на балансе
    private volatile double bybitCrypto; // $ доллары в монетах
    private volatile double bybitCoins; // BTC монеты 0,01 битка
    private volatile double kucoinUsdt;
    private volatile double kucoinCrypto;
    private volatile double kucoinCoins;
}