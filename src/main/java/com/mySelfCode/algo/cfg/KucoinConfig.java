package com.mySelfCode.algo.cfg;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "kucoin.api")
@Getter
@Setter
public class KucoinConfig {
    private String key;
    private String secret;
    private String passphrase;
    private String baseUrl;
}