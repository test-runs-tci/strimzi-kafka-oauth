package io.strimzi.kafka.oauth.common;


public class OauthAuthenticator {

    public static LoginResult loginWithClientCredentials(String tokenEndpointUrl, String clientId, String clientSecret) {
        return new LoginResult(System.getenv("OAUTH_ACCESS_TOKEN"));
    }
}
