package com.mySelfCode.algo.controller;

import com.mySelfCode.algo.dto.Status;
import com.mySelfCode.algo.entity.StopRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static reactor.netty.http.HttpConnectionLiveness.log;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StopController {
    private final Status status;
    @PostMapping("/stop")
    public ResponseEntity<?> stopAlgorithm(@RequestBody StopRequest request) {
        try {
            String password = request.getPassword();
            String truePassword = "Maxbur010!";
            if(password.equals(truePassword)) {
                log.info("Try stop algo");
                //остановить алгоритм
                status.setStatus(false);
                status.setStatusForAlgo(false);
            }else log.error("Wrong password");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error for stop algo", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
