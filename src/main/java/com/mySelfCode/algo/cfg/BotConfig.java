package com.mySelfCode.algo.cfg;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "bot")
@Getter
@Setter
public class BotConfig {
    private double kucoinFee;
    private double bybitFee;
}