package com.mySelfCode.algo.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StartRequest {
    private String password;
    private List<PairRequest> pairs;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PairRequest {
        private String symbol;
        private String percent;
    }
}