package com.mySelfCode.algo.service;

import com.mySelfCode.algo.api.bybit.BybitBuyCrypto;
import com.mySelfCode.algo.api.bybit.BybitSellCrypto;
import com.mySelfCode.algo.api.bybit.BybitTradeChecker;
import com.mySelfCode.algo.api.kucoin.KucoinBuyCrypto;
import com.mySelfCode.algo.api.kucoin.KucoinSellCrypto;
import com.mySelfCode.algo.api.kucoin.KucoinTradeChecker;
import com.mySelfCode.algo.dto.LastOrderIds;
import com.mySelfCode.algo.dto.MinMountTrade;
import com.mySelfCode.algo.dto.Profile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class Rebalance {
    private final Profile profile;
    private final LastOrderIds lastOrderIds;
    private final MinMountTrade minMountTrade;

    private final BybitBuyCrypto bybitBuyCrypto;
    private final BybitSellCrypto bybitSellCrypto;
    private final KucoinBuyCrypto kucoinBuyCrypto;
    private final KucoinSellCrypto kucoinSellCrypto;

    private final BybitTradeChecker bybitTradeChecker;
    private final KucoinTradeChecker kucoinTradeChecker;

    private volatile String bybitSellOrBuy = "";
    private volatile String kucoinSellOrBuy = "";

    public void rebalance() {
        this.bybitSellOrBuy = "";
        this.kucoinSellOrBuy = "";

        double middleBybit = (profile.getBybitUsdt() + profile.getBybitCrypto()) / 2;
        double middleKucoin = (profile.getKucoinUsdt() + profile.getKucoinCrypto()) / 2;

        //bybit
        if(profile.getBybitUsdt() > middleBybit) {
            double amountToBuy = profile.getBybitUsdt() - middleBybit;
            if (amountToBuy >= minMountTrade.getMinUsdtBybit()) {
                bybitBuyCrypto.buyMarket(amountToBuy);
                this.bybitSellOrBuy = "BUY";
            } else {
                this.bybitSellOrBuy = "NONE";
                log.info("rebalance - bybit - SKIP BUY - amount {} < min {}", amountToBuy, minMountTrade.getMinUsdtBybit());
            }
        } else if (profile.getBybitUsdt() < middleBybit) {
            double amountToSell = profile.getBybitCrypto() - middleBybit;
            if (amountToSell >= minMountTrade.getMinUsdtBybit()) {
                bybitSellCrypto.sellMarket(amountToSell);
                this.bybitSellOrBuy = "SELL";
            } else {
                this.bybitSellOrBuy = "NONE";
                log.info("rebalance - bybit - SKIP SELL - amount {} < min {}", amountToSell, minMountTrade.getMinUsdtBybit());
            }
        } else {
            this.bybitSellOrBuy = "NONE";
        }
        log.info("rebalance - bybit - {}", bybitSellOrBuy);

        //kucoin
        if(profile.getKucoinUsdt() > middleKucoin) {
            double amountToBuy = profile.getKucoinUsdt() - middleKucoin;
            if (amountToBuy >= minMountTrade.getMinUsdtKucoin()) {
                kucoinBuyCrypto.buyMarket(amountToBuy);
                this.kucoinSellOrBuy = "BUY";
            } else {
                this.kucoinSellOrBuy = "NONE";
                log.info("rebalance - kucoin - SKIP BUY - amount {} < min {}", amountToBuy, minMountTrade.getMinUsdtKucoin());
            }
        } else if (profile.getKucoinUsdt() < middleKucoin) {
            double amountToSell = profile.getKucoinCrypto() - middleKucoin;
            if (amountToSell >= minMountTrade.getMinUsdtKucoin()) {
                kucoinSellCrypto.sellMarket(amountToSell);
                this.kucoinSellOrBuy = "SELL";
            } else {
                this.kucoinSellOrBuy = "NONE";
                log.info("rebalance - kucoin - SKIP SELL - amount {} < min {}", amountToSell, minMountTrade.getMinUsdtKucoin());
            }
        } else {
            this.kucoinSellOrBuy = "NONE";
        }
        log.info("rebalance - kucoin - {}", kucoinSellOrBuy);
    }

    public String rebalanceResult() {
        if ("NONE".equals(bybitSellOrBuy) && "NONE".equals(kucoinSellOrBuy)) {
            return "Done";
        }

        String statusBybit = "Done";
        String statusKucoin = "Done";

        if ("BUY".equals(bybitSellOrBuy)) {
            String bybitId = lastOrderIds.getBybitBuyOrderId();
            if (bybitId != null && !bybitId.isEmpty()) {
                statusBybit = bybitTradeChecker.checkTrade(bybitId);
            } else {
                return "Error";
            }
        } else if ("SELL".equals(bybitSellOrBuy)) {
            String bybitId = lastOrderIds.getBybitSellOrderId();
            if (bybitId != null && !bybitId.isEmpty()) {
                statusBybit = bybitTradeChecker.checkTrade(bybitId);
            } else {
                return "Error";
            }
        }

        if ("BUY".equals(kucoinSellOrBuy)) {
            String kucoinId = lastOrderIds.getKucoinBuyOrderId();
            if (kucoinId != null && !kucoinId.isEmpty()) {
                statusKucoin = kucoinTradeChecker.checkTrade(kucoinId);
            } else {
                return "Error";
            }
        } else if ("SELL".equals(kucoinSellOrBuy)) {
            String kucoinId = lastOrderIds.getKucoinSellOrderId();
            if (kucoinId != null && !kucoinId.isEmpty()) {
                statusKucoin = kucoinTradeChecker.checkTrade(kucoinId);
            } else {
                return "Error";
            }
        }
        log.info("rebalance - status - {} - {}", statusBybit, statusKucoin);
        if (statusKucoin.equals("Done") && statusBybit.equals("Done")) return "Done";
        else if (statusKucoin.equals("InTrade") || statusBybit.equals("InTrade")) return "InTrade";
        else return "Error";
    }
}