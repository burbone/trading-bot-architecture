package com.mySelfCode.algo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AlgoApplication {
	public static void main(String[] args) {
		SpringApplication.run(AlgoApplication.class, args);
	}
}
/*
ssh -i C:\Users\Максим\.ssh\vps_key root@45.153.188.200 -t "docker logs -f trading_bot"
ton 0,1 остальное 0,2 - kucoin
0.135 - bybit
start 16.04.2026 22:10 618,34(bybit) 616,65(kucoin)
MNT USDT     0,66525  0,66475
TON USDT     1,4155   1,41525
SPX USDT     0,3467   0,34675
VIRTUAL USDT 0,7293   0,72925
 */
/*
ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIP5/o20cqVn/PfnK4IQJDN4r6Nm4zsXtlISxqfKPNW+E РњР°РєСЃРёРј@DESKTOP-SAH7IR5
 */
/*
  kucoin-fee: 0.002
  bybit-fee: 0.00135
TON USDT 0.125%
SPX USDT 0.125
VIRTUAL USDT 0.125
MNT USDT 0.125
*/

/*
P1 — цена на дорогой бирже (Bybit 0.7047)
P2 — цена на дешёвой бирже (KuCoin 0.7000)
Q — количество монет (71.43 MNT)
Fb — комса Bybit % (0.135)
Fk — комса KuCoin % (0.2)
Прибыль = Q × (P1 × (1 - Fb/100) - P2 × (1 + Fk/100))

S - спред (%)
S = ((1 + Fk/100) / (1 - Fb/100) - 1) × 100
мин = 0,3354%




 */

/*
cpu - 20%
512 - оп
net - 5mb
 */

/*
api key bybit
1lNJN0i4aScMC2E7Im

tu7qA7Z25CrS7jMXf4z1xxObWXDdQNM6mRrM
 */

/*
api key kucoin
6987661a12bd0a0001141b7e

f6ee8ff4-d59d-4777-aef4-b360ded6bfa2

(testForAlgo, 459152, Maxbur010!)
 */


/*
bybit
zhNIGHtzO5cIeVBbhr

kh4emvt1iwbtwZ70JpPniglEctOjUtkTvEYJ
 */


/*
6987c930f5eaf900017f643c
78a76f61-4690-4626-ac7f-3475a5420f02
 */