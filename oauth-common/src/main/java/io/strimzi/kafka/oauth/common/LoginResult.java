package io.strimzi.kafka.oauth.common;

import java.util.Map;

public class LoginResult {

    private String token;


    public LoginResult(String token) {
        this.token = token;
    }

    public LoginResult(Map<String, String> claims) {
        // we should have access token, sub, iat, ...
    }

    public String token() {
        return token;
    }
}
