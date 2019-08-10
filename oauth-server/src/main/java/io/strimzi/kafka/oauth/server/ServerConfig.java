package io.strimzi.kafka.oauth.server;

public class ServerConfig {

    public static final String OAUTH_SERVER_URI = "OAUTH_SERVER_URI";
    public static final String OAUTH_TOKEN_ENDPOINT_URI = "OAUTH_TOKEN_ENDPOINT_URI";
    public static final String OAUTH_CLIENT_ID = "OAUTH_CLIENT_ID";
    public static final String OAUTH_CLIENT_SECRET = "OAUTH_CLIENT_SECRET";


    private static final String authServerUri = System.getenv(OAUTH_SERVER_URI);
    private static final String tokenEndpointUri = System.getenv(OAUTH_TOKEN_ENDPOINT_URI);
    private static final String clientId = System.getenv(OAUTH_CLIENT_ID);
    private static final String clientSecret = System.getenv(OAUTH_CLIENT_SECRET);

    static String getAuthServerUri() {
        return authServerUri;
    }

    static String getTokenEndpointUri() {
        return tokenEndpointUri;
    }

    static String getClientId() {
        return clientId;
    }

    static String getClientSecret() {
        return clientSecret;
    }
}
