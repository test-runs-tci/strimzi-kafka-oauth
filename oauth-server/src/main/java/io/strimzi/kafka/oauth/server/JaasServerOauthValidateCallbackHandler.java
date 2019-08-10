package io.strimzi.kafka.oauth.server;

import io.strimzi.kafka.oauth.common.OAuthBearerTokenImpl;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerValidatorCallback;
import org.apache.kafka.common.utils.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JaasServerOauthValidateCallbackHandler implements AuthenticateCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(JaasServerOauthValidateCallbackHandler.class);

    @Override
    public void configure(Map<String, ?> map, String s, List<AppConfigurationEntry> list) {

    }

    @Override
    public void close() {

    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof OAuthBearerValidatorCallback) {
                handleCallback((OAuthBearerValidatorCallback) callback);
            } else {
                throw new UnsupportedCallbackException(callback);
            }
        }
    }

    private void handleCallback(OAuthBearerValidatorCallback callback) throws IOException {
        if (callback.tokenValue() == null) {
            throw new IllegalArgumentException("Callback has null token value!");
        }

        OAuthBearerTokenImpl token = new OAuthBearerTokenImpl(callback.tokenValue());

        if (Time.SYSTEM.milliseconds() > token.lifetimeMs())    {
            log.trace("The token expired at {}", token.lifetimeMs());
            callback.error("expired_token", null, null);
        }

        validateToken(token.value(), callback);

        if (callback.errorStatus() == null) {
            // No errors during the validation
            // We can set the token to indicate success
            callback.token(token);
        }
    }

    private void validateToken(String value, OAuthBearerValidatorCallback callback) {

    }
}
