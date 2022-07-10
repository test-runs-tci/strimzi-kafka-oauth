/*
 * Copyright 2017-2022, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.testsuite.oauth;

import com.nimbusds.jwt.SignedJWT;
import io.strimzi.kafka.oauth.common.OAuthAuthenticator;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RaceBadTokensTest {

    static final String HOST = "keycloak";
    static final String REALM = "kafka-authz";
    static final String TOKEN_ENDPOINT_URI = "http://" + HOST + ":8080/realms/" + REALM + "/protocol/openid-connect/token";

    static final String TEAM_B_CLIENT = "team-b-client";

    private static final Logger log = LoggerFactory.getLogger(RaceBadTokensTest.class);

    @Test
    public void doTest() throws InterruptedException {

        AtomicInteger goodCount = new AtomicInteger();
        AtomicInteger badCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // Hit the token endpoint with multiple threads at once
        int threadCount = 1;

        // Make it configurable using an env var
        String tc = System.getenv("KEYCLOAK_THREAD_COUNT");
        if (tc != null) {
            threadCount = Integer.parseInt(tc);
        }

        ExecutorService svc = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            svc.submit(() -> {

                // Authenticate to get the token
                try {
                    String token = OAuthAuthenticator.loginWithClientSecret(URI.create(TOKEN_ENDPOINT_URI), null, null,
                            TEAM_B_CLIENT, TEAM_B_CLIENT + "-secret", true, null, null).token();

                    // Parse the token
                    SignedJWT jwt = SignedJWT.parse(token);
                    Map<String, Object> claims = jwt.getPayload().toJSONObject();

                    // Check claims: aud, preferred_username
                    String preferredUsername = (String) claims.get("preferred_username");
                    if (preferredUsername == null) {
                        log.info("Bad token: {}", jwt.getPayload());
                        badCount.incrementAndGet();
                    } else {
                        log.info("Good token: {}", jwt.getPayload());
                        goodCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    log.error("Bad request: ", e);
                    failCount.incrementAndGet();
                }

            });
        }
        svc.shutdown();
        svc.awaitTermination(120, TimeUnit.SECONDS);

        log.info("REPORT:");
        log.info("  Good tokens:     " + goodCount.get());
        log.info("  Bad tokens:      " + badCount.get());
        log.info("  Failed requests: " + failCount.get());

        Assert.assertEquals("Keycloak returned bad tokens", 0, badCount.get());
        Assert.assertEquals("Token requests failed", 0, failCount.get());
    }
}