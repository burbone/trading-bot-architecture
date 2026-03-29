package com.mySelfCode.algo.cfg;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "bybit.api")
@Getter
@Setter
public class BybitConfig {
    private String key;
    private String secret;
    private String baseUrl;
}
