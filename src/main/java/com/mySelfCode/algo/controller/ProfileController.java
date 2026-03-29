package com.mySelfCode.algo.controller;

import com.mySelfCode.algo.dto.Profile;
import com.mySelfCode.algo.entity.ProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final Profile profile;

    @GetMapping("/bybit")
    public ResponseEntity<ProfileResponse> bybitProfile(){
        ProfileResponse profileResponse = new ProfileResponse();

        profileResponse.setUsdt(profile.getBybitUsdt());
        profileResponse.setCrypto(profile.getBybitCrypto());
        profileResponse.setCoins(profile.getBybitCoins());

        return ResponseEntity.ok(profileResponse);
    }

    @GetMapping("/kucoin")
    public ResponseEntity<ProfileResponse> kucoinProfile(){
        ProfileResponse profileResponse = new ProfileResponse();

        profileResponse.setUsdt(profile.getKucoinUsdt());
        profileResponse.setCoins(profile.getKucoinCoins());
        profileResponse.setCrypto(profile.getKucoinCrypto());
        return ResponseEntity.ok(profileResponse);
    }
}
