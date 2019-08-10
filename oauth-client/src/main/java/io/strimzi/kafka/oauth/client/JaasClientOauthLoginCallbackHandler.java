package io.strimzi.kafka.oauth.client;

import io.strimzi.kafka.oauth.common.LoginResult;
import io.strimzi.kafka.oauth.common.OAuthBearerTokenImpl;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerTokenCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static io.strimzi.kafka.oauth.client.ClientConfig.getClientId;
import static io.strimzi.kafka.oauth.client.ClientConfig.getClientSecret;
import static io.strimzi.kafka.oauth.common.OauthAuthenticator.loginWithClientCredentials;

public class JaasClientOauthLoginCallbackHandler implements AuthenticateCallbackHandler {

    @Override
    public void configure(Map<String, ?> configs, String saslMechanism, List<AppConfigurationEntry> jaasConfigEntries) {
        if (!OAuthBearerLoginModule.OAUTHBEARER_MECHANISM.equals(saslMechanism))    {
            throw new IllegalArgumentException(String.format("Unexpected SASL mechanism: %s", saslMechanism));
        }
    }

    @Override
    public void close() {

    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof OAuthBearerTokenCallback) {
                handleCallback((OAuthBearerTokenCallback) callback);
            } else {
                throw new UnsupportedCallbackException(callback);
            }
        }
    }

    private void handleCallback(OAuthBearerTokenCallback callback) throws IOException {
        if (callback.token() != null) {
            throw new IllegalArgumentException("Callback had a token already");
        }

        LoginResult result = loginWithClientCredentials(ClientConfig.getAuthServerUri(), getClientId(), getClientSecret());

        callback.token(new OAuthBearerTokenImpl(result.token()));
    }
}
