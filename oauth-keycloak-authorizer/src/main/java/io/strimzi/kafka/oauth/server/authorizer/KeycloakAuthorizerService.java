/*
 * Copyright 2017-2023, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.oauth.server.authorizer;

/**
 * A static holder for the KeycloakAuthorizerSingleton instance
 */
public class KeycloakAuthorizerService {

    private static KeycloakAuthorizerSingleton instance;

    /**
     * Get the current instance
     *
     * @return The instance previously set by {@link #setInstance(KeycloakAuthorizerSingleton)}
     */
    public static KeycloakAuthorizerSingleton getInstance() {
        return instance;
    }

    /**
     * Set the current KeycloakAuthorizerSingleton instance
     *
     * @param instance The new instance
     */
    public static void setInstance(KeycloakAuthorizerSingleton instance) {
        KeycloakAuthorizerService.instance = instance;
    }
}
