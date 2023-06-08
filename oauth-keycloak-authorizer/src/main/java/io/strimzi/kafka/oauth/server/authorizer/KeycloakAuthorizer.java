/*
 * Copyright 2017-2023, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.oauth.server.authorizer;

import org.apache.kafka.common.Endpoint;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.metadata.authorizer.AclMutator;
import org.apache.kafka.metadata.authorizer.ClusterMetadataAuthorizer;
import org.apache.kafka.metadata.authorizer.StandardAcl;
import org.apache.kafka.server.authorizer.Action;
import org.apache.kafka.server.authorizer.AuthorizableRequestContext;
import org.apache.kafka.server.authorizer.AuthorizationResult;
import org.apache.kafka.server.authorizer.AuthorizerServerInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An authorizer that ensures a single stateful instance is used for authorization callbacks
 */
public class KeycloakAuthorizer implements ClusterMetadataAuthorizer {

    private KeycloakAuthorizerSingleton delegate;

    @Override
    public void configure(Map<String, ?> configs) {
        System.out.println(this + " - configure()");
        Configuration configuration = new Configuration(configs);
        KeycloakAuthorizerSingleton singleton = KeycloakAuthorizerService.getInstance();
        if (singleton != null) {
            if (configuration.equals(singleton.getConfiguration())) {
                delegate = singleton;
            }
        }

        if (delegate == null) {
            singleton = new KeycloakAuthorizerSingleton();
            singleton.configure(configs);
            KeycloakAuthorizerService.setInstance(singleton);
            delegate = singleton;
        }
    }

    @Override
    public void setAclMutator(AclMutator aclMutator) {
        //if (delegate == null) {
        //    throw new IllegalStateException("KeycloakAuthorizer has not been configured");
        //}
        System.out.println(this + " - setAclMutator()");
        if (delegate != null) {
            delegate.setAclMutator(aclMutator);
        }
    }

    @Override
    public AclMutator aclMutatorOrException() {
        //if (delegate == null) {
        //    throw new IllegalStateException("KeycloakAuthorizer has not been configured");
        //}
        System.out.println(this + " - aclMutatorOrException()");
        if (delegate != null) {
            return delegate.aclMutatorOrException();
        }
        throw new IllegalStateException("KeycloakAuthorizer has not been configured");
    }

    @Override
    public void completeInitialLoad() {
        //if (delegate == null) {
        //    throw new IllegalStateException("KeycloakAuthorizer has not been configured");
        //}
        System.out.println(this + " - completeInitialLoad()");
        if (delegate != null) {
            delegate.completeInitialLoad();
        }
    }

    @Override
    public void completeInitialLoad(Exception e) {
        //if (delegate == null) {
        //    throw new IllegalStateException("KeycloakAuthorizer has not been configured");
        //}
        System.out.println(this + " - completeInitialLoad(e): " + e);
        if (e != null) {
            e.printStackTrace();
        }
        if (delegate != null) {
            delegate.completeInitialLoad(e);
        }
    }

    @Override
    public void loadSnapshot(Map<Uuid, StandardAcl> acls) {
        //if (delegate == null) {
        //    throw new IllegalStateException("KeycloakAuthorizer has not been configured");
        //}
        System.out.println(this + " - loadSnapshot(acls): " + acls);
        if (delegate != null) {
            delegate.loadSnapshot(acls);
        }
    }

    @Override
    public void addAcl(Uuid id, StandardAcl acl) {
        //if (delegate == null) {
        //    throw new IllegalStateException("KeycloakAuthorizer has not been configured");
        //}
        System.out.println(this + " - addAcl(id, acl): id=" + id + ", acl=" + acl);
        if (delegate != null) {
            delegate.addAcl(id, acl);
        }
    }

    @Override
    public void removeAcl(Uuid id) {
        //if (delegate == null) {
        //    throw new IllegalStateException("KeycloakAuthorizer has not been configured");
        //}
        System.out.println(this + " - removeAcl(id): " + id);
        if (delegate != null) {
            delegate.removeAcl(id);
        }
    }

    @Override
    public Map<Endpoint, ? extends CompletionStage<Void>> start(AuthorizerServerInfo serverInfo) {
        //if (delegate != null) {
        //    throw new IllegalStateException("KeycloakAuthorizer has not been configured");
        //}
        System.out.println(this + " - start(serverInfo): " + serverInfo);
        if (delegate != null) {
            return delegate.start(serverInfo);
        }

        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        return serverInfo.endpoints().stream().collect(Collectors.toMap(Function.identity(), e -> future));
    }

    @Override
    public List<AuthorizationResult> authorize(AuthorizableRequestContext requestContext, List<Action> actions) {
        if (delegate == null) {
            throw new IllegalStateException("KeycloakAuthorizer has not been configured");
        }
        return delegate.authorize(requestContext, actions);
    }

    @Override
    public Iterable<AclBinding> acls(AclBindingFilter filter) {
        if (delegate == null) {
            throw new IllegalStateException("KeycloakAuthorizer has not been configured");
        }
        return delegate.acls(filter);
    }

    @Override
    public void close() {
        if (delegate != null) {
            delegate.close();
        }
    }
}
