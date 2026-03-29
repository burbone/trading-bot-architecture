package com.mySelfCode.algo.service;

import com.mySelfCode.algo.api.bybit.BybitBuyCrypto;
import com.mySelfCode.algo.api.bybit.BybitSellCrypto;
import com.mySelfCode.algo.api.kucoin.KucoinBuyCrypto;
import com.mySelfCode.algo.api.kucoin.KucoinSellCrypto;
import com.mySelfCode.algo.dto.Profile;
import com.mySelfCode.algo.dto.StarterInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Getter
public class Trade {
    private final Profile profile;
    private final BybitSellCrypto bybitSellCrypto;
    private final BybitBuyCrypto bybitBuyCrypto;
    private final KucoinSellCrypto kucoinSellCrypto;
    private final KucoinBuyCrypto kucoinBuyCrypto;

    public void sellAllCryptoBybit() {
        bybitSellCrypto.sellMarket(profile.getBybitCrypto());
    }

    public void buyAllCryptoBybit(){
        bybitBuyCrypto.buyMarket(profile.getBybitUsdt());
    }

    public void sellAllCryptoKucoin() {
        kucoinSellCrypto.sellMarket(profile.getKucoinCrypto());
    }

    public void buyAllCryptoKucoin(){
        kucoinBuyCrypto.buyMarket(profile.getKucoinUsdt());
    }
}
