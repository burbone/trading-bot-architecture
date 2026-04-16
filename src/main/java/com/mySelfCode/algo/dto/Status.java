package com.mySelfCode.algo.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class Status {
    private volatile boolean status = false;
    private volatile boolean statusForAlgo = false;
    private volatile boolean globalRebalanceDone = false;
}