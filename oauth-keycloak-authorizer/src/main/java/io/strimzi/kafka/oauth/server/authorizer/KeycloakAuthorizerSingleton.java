/*
 * Copyright 2017-2023, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.oauth.server.authorizer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.strimzi.kafka.oauth.common.ConfigException;
import io.strimzi.kafka.oauth.server.OAuthKafkaPrincipalBuilder;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.metadata.authorizer.AclMutator;
import org.apache.kafka.metadata.authorizer.ClusterMetadataAuthorizer;
import org.apache.kafka.metadata.authorizer.StandardAcl;
import org.apache.kafka.metadata.authorizer.StandardAuthorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * An authorizer that grants access based on security policies managed in Keycloak Authorization Services.
 * This is a drop-in replacement for <code>KeycloakRBACAuthorizer</code> with functionality mostly inherited from there.
 * <p>
 * This implementation supports KRaft mode and delegates to <code>org.apache.kafka.metadata.authorizer.StandardAuthorizer</code>
 * if <code>strimzi.authorization.delegate.to.kafka.acl</code> is set to <code>true</code>.
 * <p>
 * This authorizer auto-detects whether the broker runs in KRaft mode or not based on the presence and value of <code>process.roles</code> config option.
 * <p>
 * KeycloakAuthorizer works in conjunction with JaasServerOauthValidatorCallbackHandler, and requires
 * {@link OAuthKafkaPrincipalBuilder} to be configured as 'principal.builder.class' in 'server.properties' file.
 * <p>
 * To install this authorizer in Kafka, specify the following in your 'server.properties':
 * </p>
 * <pre>
 *     authorizer.class.name=io.strimzi.kafka.oauth.server.authorizer.KeycloakAuthorizer
 *     principal.builder.class=io.strimzi.kafka.oauth.server.OAuthKafkaPrincipalBuilder
 * </pre>
 *
 * For more configuration options see README.md and {@link io.strimzi.kafka.oauth.server.authorizer.KeycloakRBACAuthorizer}.
 */
public class KeycloakAuthorizerSingleton extends KeycloakRBACAuthorizer implements ClusterMetadataAuthorizer {

    static final Logger log = LoggerFactory.getLogger(KeycloakAuthorizerSingleton.class);

    private ClusterMetadataAuthorizer kraftAuthorizer;

    private AclMutator mutator;


    @Override
    void setupDelegateAuthorizer(Map<String, ?> configs) {
        if (getConfiguration().isKRaft()) {
            try {
                kraftAuthorizer = new StandardAuthorizer();
                setDelegate(kraftAuthorizer);
                log.debug("Using StandardAuthorizer (KRaft based) as a delegate");
            } catch (Exception e) {
                throw new ConfigException("KRaft mode detected ('process.roles' configured), but failed to instantiate org.apache.kafka.metadata.authorizer.StandardAuthorizer", e);
            }
        }
        super.setupDelegateAuthorizer(configs);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    // See https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html#ei2-may-expose-internal-representation-by-incorporating-reference-to-mutable-object-ei-expose-rep2
    public void setAclMutator(AclMutator aclMutator) {
        if (kraftAuthorizer != null) {
            kraftAuthorizer.setAclMutator(aclMutator);
        }
        this.mutator = aclMutator;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    // See https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html#ei-may-expose-internal-representation-by-returning-reference-to-mutable-object-ei-expose-rep
    public AclMutator aclMutatorOrException() {
        if (kraftAuthorizer != null) {
            return kraftAuthorizer.aclMutatorOrException();
        }
        return mutator;
    }

    public void completeInitialLoad() {
        if (kraftAuthorizer != null) {
            kraftAuthorizer.completeInitialLoad();
        }
    }

    public void completeInitialLoad(Exception e) {
        if (kraftAuthorizer != null) {
            kraftAuthorizer.completeInitialLoad(e);
        }
        log.error("Failed to load authorizer cluster metadata", e);
    }

    public void loadSnapshot(Map<Uuid, StandardAcl> acls) {
        if (kraftAuthorizer != null) {
            kraftAuthorizer.loadSnapshot(acls);
        }
    }

    public void addAcl(Uuid id, StandardAcl acl) {
        if (kraftAuthorizer == null) {
            throw new UnsupportedOperationException("StandardAuthorizer ACL delegation not enabled");
        }
        kraftAuthorizer.addAcl(id, acl);
    }

    public void removeAcl(Uuid id) {
        if (kraftAuthorizer == null) {
            throw new UnsupportedOperationException("StandardAuthorizer ACL delegation not enabled");
        }
        kraftAuthorizer.removeAcl(id);
    }
}
