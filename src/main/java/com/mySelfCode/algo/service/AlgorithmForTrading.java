package com.mySelfCode.algo.service;

import com.mySelfCode.algo.api.bybit.BybitTradeChecker;
import com.mySelfCode.algo.api.kucoin.KucoinTradeChecker;
import com.mySelfCode.algo.dto.*;
import com.mySelfCode.algo.entity.HistoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlgorithmForTrading {
    private volatile String process = "NON";

    private final BybitData bybitData;
    private final KucoinData kucoinData;
    private final Rebalance rebalance;
    private final StarterInfo starterInfo;
    private final Trade trade;
    private final BybitTradeChecker bybitTradeChecker;
    private final KucoinTradeChecker kucoinTradeChecker;
    private final HistoryResponse historyResponse;
    private final Status status;
    private final LastOrderIds lastOrderIds;

    private int wait = 0;
    private boolean check = false;
    private boolean bybit = false;
    private int schetBybit = 0;
    private int schetKucoin = 0;

    private int waitAlgoLog = 0;
    private int waitErrorLog = 0;

    @Scheduled(fixedRate = 1000)
    public void algorithmForTrading() {
        if (status.isStatusForAlgo()) {
            if (process.equals("NON") || process.equals("READY_TO_EXIT_FROM_ALGO")) {
                //rebalance logic

                if (this.check == false) {
                    //делаем ребаланс
                    rebalance.rebalance();
                    log.info("Rebalance");

                    this.check = true;
                    wait = 15;
                } else {
                    String result = rebalance.rebalanceResult();
                    if (wait > 0) {
                        if (result.equals("Done")) {
                            //если получилось
                            process = "READY_FOR_ALGO";
                            this.check = false;

                            History history = new History();
                            history.setType("R");
                            history.setBybitPrice((bybitData.getAskPrice() + bybitData.getBidPrice()) / 2);
                            history.setKucoinPrice((kucoinData.getAskPrice() + kucoinData.getBidPrice()) / 2);
                            historyResponse.addToHistory(history);
                            log.info("Rebalance done");
                        } else if (result.equals("Error")) {
                            //если ошибка
                            process = "ERROR";
                            this.check = false;
                        }
                        wait--;
                    } else {
                        process = "ERROR";
                    }
                }
            }

            if (process.equals("READY_FOR_ALGO")) {
                bybit = false;
                //разница цен, делаем вход
                if (this.check == false) {
                    if (waitAlgoLog > 0) {
                        this.waitAlgoLog--;
                    } else {
                       this.waitAlgoLog = 60;
                        log.info("algo - wait");
                    }
                    if (bybitData.getBidPrice() >= kucoinData.getAskPrice() * (1 + (starterInfo.getPercent() / 100))) {
                        //продаем на байбит 200-0, покупаем на кукоин
                        this.schetBybit = schetBybit + 1;
                        bybit = true;
                        wait = 15;
                        this.check = true;
                        trade.sellAllCryptoBybit();
                        trade.buyAllCryptoKucoin();

                    } else if (kucoinData.getBidPrice() >= bybitData.getAskPrice() * (1 + (starterInfo.getPercent() / 100))) {
                        //продаем на кукоин 200-0, покупаем на байбит
                        this.schetKucoin = schetKucoin + 1;
                        bybit = false;
                        wait = 15;
                        this.check = true;
                        trade.buyAllCryptoBybit();
                        trade.sellAllCryptoKucoin();

                    }
                } else {
                    log.info("algo - enter in");
                    if (wait > 0) {
                        String bybitAns = "";
                        String kucoinAns = "";
                        if (bybit) {
                            bybitAns = bybitTradeChecker.checkTrade(lastOrderIds.getBybitSellOrderId());
                            kucoinAns = kucoinTradeChecker.checkTrade(lastOrderIds.getKucoinBuyOrderId());
                        } else {
                            bybitAns = bybitTradeChecker.checkTrade(lastOrderIds.getBybitBuyOrderId());
                            kucoinAns = kucoinTradeChecker.checkTrade(lastOrderIds.getKucoinSellOrderId());
                        }
                        if (bybitAns.equals("Done") && kucoinAns.equals("Done")) {
                            //если все выполнено
                            this.check = false;
                            process = "IN_ALGO";

                            History history = new History();
                            history.setType("D");
                            history.setBybitPrice((bybitData.getAskPrice() + bybitData.getBidPrice()) / 2);
                            history.setKucoinPrice((kucoinData.getAskPrice() + kucoinData.getBidPrice()) / 2);
                            historyResponse.addToHistory(history);
                        } else if (bybitAns.equals("Error") || kucoinAns.equals("Error")) {
                            //если хоть 1 ошибка
                            this.check = false;
                            process = "ERROR";
                        }
                        wait--;
                    } else {
                        wait = 15;
                        process = "ERROR";
                    }
                }
            }

            if (process.equals("IN_ALGO")) {
                //логика выхода с алгоритм
                log.info("algo - in");
                double midBybit = (bybitData.getAskPrice() + bybitData.getBidPrice()) / 2;
                double midKucoin = (kucoinData.getAskPrice() + kucoinData.getBidPrice()) / 2;
                if (Math.abs(midBybit - midKucoin) / midBybit * 100 <= 0.001) {
                    History history = new History();
                    history.setType("W");
                    history.setBybitPrice((bybitData.getAskPrice() + bybitData.getBidPrice()) / 2);
                    history.setKucoinPrice((kucoinData.getAskPrice() + kucoinData.getBidPrice()) / 2);
                    historyResponse.addToHistory(history);
                    this.process = "READY_TO_EXIT_FROM_ALGO";
                }
            }

            if (process.equals("ERROR")) {
                if (waitErrorLog > 0) {
                    this.waitErrorLog--;
                } else {
                    this.waitErrorLog = 60;
                    History history = new History();
                    history.setType("ERROR");
                    history.setBybitPrice(0);
                    history.setKucoinPrice(0);
                    historyResponse.addToHistory(history);
                    log.error("ERROR ALGO, need HELP");
                }
            }
        }
    }
}
/*
ожидания сделок или чего-то такого всегда 15 секунд, если какая-то ошибка или баг выводится error который по сути стопает все, и не важно что происходит - главное код больше не посылает запросы и требует ручного вмешательства

логика сама по себе проста
есть 2 вида пути, стартовый который 1 раз что-то делает и продолжающий по кругу
старт - ребаланс - ожидание входа - вход - ожидание выхода - ребаланс (выход)
после этого все проще Ребаланс - ожидание входа - вход - ожидание выхода и заново
суть - ребаланс выравнивание цен один раз и сразу переход в ожидание если ребаланс прошел успешно
ожидание входа - ожидание расхождения цен на биржах на процент + комиссию
вход - полная покупка на дешевой бирже и полная продажа на дорогой бирже
ожидание выхода - схождение цен с погрешностью в 0,01%

везде где задействуется покупка или продажа происходит проверка на выполнение сделки и ожидание пока не вернется, что сделка прошла, только после этого переходит на следующий этап
 */