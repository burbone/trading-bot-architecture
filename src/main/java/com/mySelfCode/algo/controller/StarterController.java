package com.mySelfCode.algo.controller;

import com.mySelfCode.algo.api.bybit.BybitInstrumentInfo;
import com.mySelfCode.algo.api.kucoin.KucoinInstrumentInfo;
import com.mySelfCode.algo.cfg.BotConfig;
import com.mySelfCode.algo.dto.MinMountTrade;
import com.mySelfCode.algo.dto.StarterInfo;
import com.mySelfCode.algo.dto.Status;
import com.mySelfCode.algo.entity.StartRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StarterController {

    private final Status status;
    private final StarterInfo starterInfo;
    private final BotConfig botConfig;
    private final MinMountTrade minMountTrade;
    private final BybitInstrumentInfo bybitInstrumentInfo;
    private final KucoinInstrumentInfo kucoinInstrumentInfo;

    private static final Logger log = LoggerFactory.getLogger(StarterController.class);

    @PostMapping("/starter")
    public ResponseEntity<?> startAlgorithm(@RequestBody StartRequest request) {
        try {
            String password = request.getPassword();
            String truePassword = "Maxbur010!";
            if (password.equals(truePassword)) {
                log.info("Take info for start: symbol={}, percent={}",
                        request.getSymbol(), request.getPercent());

                //запись символа и процента
                //0.11 BTC USDT
                String symbol = request.getSymbol();
                double percent = Double.parseDouble(request.getPercent());
                double threshold = percent + botConfig.getBybitFee() * 100 + botConfig.getKucoinFee() * 100;
                log.info("percent = {}, with comission = {}", percent, threshold);
                //присвоение
                starterInfo.setPercent(threshold);
                starterInfo.setSymbol(symbol);

                //запись минималок
                double[] bybitInfo = bybitInstrumentInfo.getMinTradeInfo(symbol);
                double[] kucoinInfo = kucoinInstrumentInfo.getMinTradeInfo(symbol);

                minMountTrade.setMinCryptoBybit(bybitInfo[0]);
                minMountTrade.setMinUsdtBybit(bybitInfo[1]);
                minMountTrade.setMinCyptoPrecisionBybit((int) Math.round(-Math.log10(bybitInfo[2])));
                minMountTrade.setMinUsdtPrecisionBybit((int) Math.round(-Math.log10(bybitInfo[3])));

                minMountTrade.setMinCryptoKucoin(kucoinInfo[0]);
                minMountTrade.setMinUsdtKucoin(kucoinInfo[1]);
                minMountTrade.setMinCyptoPrecisionKucoin((int) Math.round(-Math.log10(kucoinInfo[2])));
                minMountTrade.setMinUsdtPrecisionKucoin((int) Math.round(-Math.log10(kucoinInfo[3])));

                // Запуск алгоритма

                status.setStatus(true);
                new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                        status.setStatusForAlgo(true);
                    } catch (InterruptedException e) {
                        log.error("Algo - start - ERROR");
                        Thread.currentThread().interrupt();
                    }
                }).start();


            } else {
                log.error("Wrong password");
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error for start algo", e);
            return ResponseEntity.internalServerError().build();

        }
    }
}
