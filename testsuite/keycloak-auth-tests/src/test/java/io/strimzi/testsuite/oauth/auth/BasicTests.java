/*
 * Copyright 2017-2020, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.testsuite.oauth.auth;

import io.strimzi.kafka.oauth.client.ClientConfig;
import io.strimzi.kafka.oauth.common.TokenInfo;
import io.strimzi.testsuite.oauth.common.TestMetrics;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.Assert;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static io.strimzi.kafka.oauth.common.OAuthAuthenticator.loginWithClientSecret;
import static io.strimzi.testsuite.oauth.auth.Common.buildConsumerConfigOAuthBearer;
import static io.strimzi.testsuite.oauth.auth.Common.buildProducerConfigOAuthBearer;
import static io.strimzi.testsuite.oauth.auth.Common.loginWithUsernameForRefreshToken;
import static io.strimzi.testsuite.oauth.auth.Common.poll;
import static io.strimzi.testsuite.oauth.common.TestMetrics.getPrometheusMetrics;
import static io.strimzi.testsuite.oauth.common.TestUtil.getKafkaLogsForString;
import static java.util.Collections.singletonList;

public class BasicTests {

    static void doTests() throws Exception {
        oauthMetricsConfigIntegration();
        oauthMetricsClientAuth();
        clientCredentialsWithJwtECDSAValidation();
        clientCredentialsWithJwtRSAValidation();
        accessTokenWithIntrospection();
        refreshTokenWithIntrospection();
    }

    static void oauthMetricsConfigIntegration() {
        System.out.println("==== KeycloakAuthenticationTest :: oauthMetricsConfigIntegrationTest ====");

        // Test MetricReporter config works as expected
        // Get kafka log and make sure the TestMetricReporter was initialised exactly twice
        List<String> lines = getKafkaLogsForString("TestMetricsReporter no. ");
        Assert.assertEquals("Kafka log should contain: \"TestMetricsReporter no. \" exactly twice", 2, lines.size());
        Assert.assertTrue("Contains \"TestMetricsReporter no. 1\"", lines.get(0).contains("TestMetricsReporter no. 1 "));
        Assert.assertTrue("Contains \"TestMetricsReporter no. 2\"", lines.get(1).contains("TestMetricsReporter no. 2 "));

        // Ensure the configuration was applied as expected
        lines = getKafkaLogsForString("Creating Metrics:");
        String line = lines.get(1);
        Assert.assertTrue("samples: 3", line.contains("samples: 3"));
        Assert.assertTrue("recordingLevel: DEBUG", line.contains("recordingLevel: DEBUG"));
        Assert.assertTrue("timeWindowMs: 15000", line.contains("timeWindowMs: 15000"));

        line = lines.get(2);
        Assert.assertTrue("test.label=testvalue", line.contains("test.label=testvalue"));
        Assert.assertTrue("_namespace=strimzi.oauth", line.contains("_namespace=strimzi.oauth"));
        Assert.assertTrue("kafka.broker.id=1", line.contains("kafka.broker.id=1"));

        line = lines.get(3);
        Assert.assertTrue("io.strimzi.testsuite.oauth.auth.metrics.TestMetricsReporter", line.contains("io.strimzi.testsuite.oauth.auth.metrics.TestMetricsReporter"));
        Assert.assertTrue("org.apache.kafka.common.metrics.JmxReporter", line.contains("org.apache.kafka.common.metrics.JmxReporter"));
    }

    static void oauthMetricsClientAuth() throws Exception {

        System.out.println("==== KeycloakAuthenticationTest :: oauthMetricsClientAuthTest ====");

        final String authHostPort = "keycloak:8080";
        final String realm = "demo";
        final String tokenPath = "/realms/" + realm + "/protocol/openid-connect/token";

        // Inter-broker communication uses INTROSPECT listener
        // We use that listener to test client authentication metrics. There are 2 inter-broker client connections established.
        TestMetrics metrics = getPrometheusMetrics(URI.create("http://kafka:9404/metrics"));

        // Request for token from login callback handler
        BigDecimal value = metrics.getValueSum("strimzi_oauth_authentication_requests_count", "context", "INTROSPECT", "kind", "client-auth", "outcome", "success");
        Assert.assertEquals("strimzi_oauth_authentication_requests_count for client-auth == 2", 2, value.intValue());

        value = metrics.getValueSum("strimzi_oauth_authentication_requests_totaltimems", "context", "INTROSPECT", "kind", "client-auth", "outcome", "success");
        Assert.assertTrue("strimzi_oauth_authentication_requests_totaltimems for client-auth > 0.0", value.doubleValue() > 0.0);

        value = metrics.getValueSum("strimzi_oauth_authentication_requests_avgtimems", "context", "INTROSPECT", "kind", "client-auth", "outcome", "success");
        Assert.assertTrue("strimzi_oauth_authentication_requests_avgtimems for client-auth > 0.0", value.doubleValue() > 0.0);

        value = metrics.getValueSum("strimzi_oauth_authentication_requests_mintimems", "context", "INTROSPECT", "kind", "client-auth", "outcome", "success");
        Assert.assertTrue("strimzi_oauth_authentication_requests_mintimems for client-auth > 0.0", value.doubleValue() > 0.0);

        value = metrics.getValueSum("strimzi_oauth_authentication_requests_maxtimems", "context", "INTROSPECT", "kind", "client-auth", "outcome", "success");
        Assert.assertTrue("strimzi_oauth_authentication_requests_maxtimems for client-auth > 0.0", value.doubleValue() > 0.0);

        // Authentication to keycloak to exchange clientId + cesret for an access token during login callback handler call
        value = metrics.getValueSum("strimzi_oauth_http_requests_count", "context", "INTROSPECT", "kind", "client-auth", "host", authHostPort, "path", tokenPath, "outcome", "success");
        Assert.assertEquals("strimzi_oauth_http_requests_count for client-auth == 2", 2, value.intValue());

        value = metrics.getValueSum("strimzi_oauth_http_requests_totaltimems", "context", "INTROSPECT", "kind", "client-auth", "host", authHostPort, "path", tokenPath, "outcome", "success");
        Assert.assertTrue("strimzi_oauth_http_requests_totaltimems for client-auth > 0.0", value.doubleValue() > 0.0);

        value = metrics.getValueSum("strimzi_oauth_http_requests_avgtimems", "context", "INTROSPECT", "kind", "client-auth", "host", authHostPort, "path", tokenPath, "outcome", "success");
        Assert.assertTrue("strimzi_oauth_http_requests_avgtimems for client-auth > 0.0", value.doubleValue() > 0.0);

        value = metrics.getValueSum("strimzi_oauth_http_requests_mintimems", "context", "INTROSPECT", "kind", "client-auth", "host", authHostPort, "path", tokenPath, "outcome", "success");
        Assert.assertTrue("strimzi_oauth_http_requests_mintimems for client-auth > 0.0", value.doubleValue() > 0.0);

        value = metrics.getValueSum("strimzi_oauth_http_requests_maxtimems", "context", "INTROSPECT", "kind", "client-auth", "host", authHostPort, "path", tokenPath, "outcome", "success");
        Assert.assertTrue("strimzi_oauth_http_requests_maxtimems for client-auth > 0.0", value.doubleValue() > 0.0);
    }

    static void clientCredentialsWithJwtECDSAValidation() throws Exception {

        System.out.println("==== KeycloakAuthenticationTest :: clientCredentialsWithJwtECDSAValidationTest ====");

        final String kafkaBootstrap = "kafka:9092";
        final String authHostPort = "keycloak:8080";
        final String realm = "demo-ec";
        final String path = "/realms/" + realm + "/protocol/openid-connect/token";

        final String tokenEndpointUri = "http://" + authHostPort + path;

        // For metrics
        final String jwksPath = "/realms/" + realm + "/protocol/openid-connect/certs";

        Map<String, String> oauthConfig = new HashMap<>();
        oauthConfig.put(ClientConfig.OAUTH_TOKEN_ENDPOINT_URI, tokenEndpointUri);
        oauthConfig.put(ClientConfig.OAUTH_CLIENT_ID, "kafka-producer-client");
        oauthConfig.put(ClientConfig.OAUTH_CLIENT_SECRET, "kafka-producer-client-secret");
        oauthConfig.put(ClientConfig.OAUTH_USERNAME_CLAIM, "preferred_username");

        Properties producerProps = buildProducerConfigOAuthBearer(kafkaBootstrap, oauthConfig);
        Producer<String, String> producer = new KafkaProducer<>(producerProps);

        final String topic = "KeycloakAuthenticationTest-clientCredentialsWithJwtECDSAValidationTest";


        producer.send(new ProducerRecord<>(topic, "The Message")).get();
        System.out.println("Produced The Message");

        Properties consumerProps = buildConsumerConfigOAuthBearer(kafkaBootstrap, oauthConfig);
        Consumer<String, String> consumer = new KafkaConsumer<>(consumerProps);

        TopicPartition partition = new TopicPartition(topic, 0);
        consumer.assign(singletonList(partition));

        while (consumer.partitionsFor(topic, Duration.ofSeconds(1)).size() == 0) {
            System.out.println("No assignment yet for consumer");
        }
        consumer.seekToBeginning(singletonList(partition));

        ConsumerRecords<String, String> records = poll(consumer);

        Assert.assertEquals("Got message", 1, records.count());
        Assert.assertEquals("Is message text: 'The Message'", "The Message", records.iterator().next().value());

        // Check metrics

        TestMetrics metrics = getPrometheusMetrics(URI.create("http://kafka:9404/metrics"));
        BigDecimal value = metrics.getValueSum("strimzi_oauth_http_requests_count", "kind", "jwks", "host", authHostPort, "path", jwksPath, "outcome", "success");
        Assert.assertTrue("strimzi_oauth_http_requests_count for jwks > 0", value.doubleValue() > 0.0);

        value = metrics.getValueSum("strimzi_oauth_http_requests_totaltimems", "kind", "jwks", "host", authHostPort, "path", jwksPath, "outcome", "success");
        Assert.assertTrue("strimzi_oauth_http_requests_totaltimems for jwks > 0.0", value.doubleValue() > 0.0);

        value = metrics.getValueSum("strimzi_oauth_validation_requests_count", "context", "JWT", "kind", "jwks", "mechanism", "OAUTHBEARER", "outcome", "success");
        // There is no inter-broker connection on this listener, producer did 2 validations, and consumer also did 2 validations
        Assert.assertTrue("strimzi_oauth_validation_requests_count for jwks >= 4", value != null && value.intValue() >= 4);

        value = metrics.getValueSum("strimzi_oauth_validation_requests_totaltimems", "context", "JWT", "kind", "jwks", "mechanism", "OAUTHBEARER", "outcome", "success");
        Assert.assertTrue("strimzi_oauth_http_requests_totaltimems for jwks > 0.0", value.doubleValue() > 0.0);
    }

    /**
     * This test uses the Kafka listener configured with both OAUTHBEARER and PLAIN, and the Keycloak realm
     * that uses the default RSA cryptography to sign tokens.
     *
     * It connects to the Kafka using the OAUTHBEARER mechanism
     *
     * @throws Exception Any unhandled error
     */
    static void clientCredentialsWithJwtRSAValidation() throws Exception {

        System.out.println("==== KeycloakAuthenticationTest :: clientCredentialsWithJwtRSAValidation ====");

        final String kafkaBootstrap = "kafka:9096";
        final String authHostPort = "keycloak:8080";
        final String realm = "kafka-authz";
        final String path = "/realms/" + realm + "/protocol/openid-connect/token";

        final String tokenEndpointUri = "http://" + authHostPort + path;

        // For metrics
        String jwksPath = "/realms/" + realm + "/protocol/openid-connect/certs";

        Map<String, String> oauthConfig = new HashMap<>();
        oauthConfig.put(ClientConfig.OAUTH_TOKEN_ENDPOINT_URI, tokenEndpointUri);
        oauthConfig.put(ClientConfig.OAUTH_CLIENT_ID, "team-a-client");
        oauthConfig.put(ClientConfig.OAUTH_CLIENT_SECRET, "team-a-client-secret");
        oauthConfig.put(ClientConfig.OAUTH_USERNAME_CLAIM, "preferred_username");

        Properties producerProps = buildProducerConfigOAuthBearer(kafkaBootstrap, oauthConfig);
        Producer<String, String> producer = new KafkaProducer<>(producerProps);

        final String topic = "KeycloakAuthenticationTest-clientCredentialsWithJwtRSAValidationTest";


        producer.send(new ProducerRecord<>(topic, "The Message")).get();
        System.out.println("Produced The Message");

        Properties consumerProps = buildConsumerConfigOAuthBearer(kafkaBootstrap, oauthConfig);
        Consumer<String, String> consumer = new KafkaConsumer<>(consumerProps);

        TopicPartition partition = new TopicPartition(topic, 0);
        consumer.assign(singletonList(partition));

        while (consumer.partitionsFor(topic, Duration.ofSeconds(1)).size() == 0) {
            System.out.println("No assignment yet for consumer");
        }
        consumer.seekToBeginning(singletonList(partition));

        ConsumerRecords<String, String> records = poll(consumer);

        Assert.assertEquals("Got message", 1, records.count());
        Assert.assertEquals("Is message text: 'The Message'", "The Message", records.iterator().next().value());

        // Check metrics

        TestMetrics metrics = getPrometheusMetrics(URI.create("http://kafka:9404/metrics"));
        BigDecimal value = metrics.getValueSum("strimzi_oauth_validation_requests_count", "context", "JWTPLAIN", "kind", "jwks", "host", authHostPort, "path", jwksPath, "mechanism", "OAUTHBEARER", "outcome", "success");

        // There is no inter-broker connection on this listener, producer did 2 validations, and consumer also did 2
        Assert.assertTrue("strimzi_oauth_validation_requests_count for jwks >= 4", value != null && value.intValue() >= 4);

        value = metrics.getValueSum("strimzi_oauth_validation_requests_totaltimems", "context", "JWTPLAIN", "kind", "jwks", "host", authHostPort, "path", jwksPath, "mechanism", "OAUTHBEARER", "outcome", "success");
        Assert.assertTrue("strimzi_oauth_validation_requests_totaltimems for jwks > 0.0", value.doubleValue() > 0.0);
    }

    static void accessTokenWithIntrospection() throws Exception {
        System.out.println("==== KeycloakAuthenticationTest :: accessTokenWithIntrospectionTest ====");

        final String kafkaBootstrap = "kafka:9093";
        final String authHostPort = "keycloak:8080";
        final String realm = "demo";
        final String path = "/realms/" + realm + "/protocol/openid-connect/token";

        // For metrics
        final String introspectPath = "/realms/" + realm + "/protocol/openid-connect/token/introspect";

        final String tokenEndpointUri = "http://" + authHostPort + path;
        final String clientId = "kafka-producer-client";
        final String clientSecret = "kafka-producer-client-secret";

        // First, request access token using client id and secret
        TokenInfo info = loginWithClientSecret(URI.create(tokenEndpointUri), null, null, clientId, clientSecret, true, null, null);

        Map<String, String> oauthConfig = new HashMap<>();
        oauthConfig.put(ClientConfig.OAUTH_ACCESS_TOKEN, info.token());
        oauthConfig.put(ClientConfig.OAUTH_USERNAME_CLAIM, "preferred_username");

        Properties producerProps = buildProducerConfigOAuthBearer(kafkaBootstrap, oauthConfig);
        Producer<String, String> producer = new KafkaProducer<>(producerProps);


        final String topic = "KeycloakAuthenticationTest-accessTokenWithIntrospectionTest";

        producer.send(new ProducerRecord<>(topic, "The Message")).get();
        System.out.println("Produced The Message");

        Properties consumerProps = buildConsumerConfigOAuthBearer(kafkaBootstrap, oauthConfig);
        Consumer<String, String> consumer = new KafkaConsumer<>(consumerProps);

        TopicPartition partition = new TopicPartition(topic, 0);
        consumer.assign(singletonList(partition));

        while (consumer.partitionsFor(topic, Duration.ofSeconds(1)).size() == 0) {
            System.out.println("No assignment yet for consumer");
        }
        consumer.seekToBeginning(singletonList(partition));

        ConsumerRecords<String, String> records = poll(consumer);

        Assert.assertEquals("Got message", 1, records.count());
        Assert.assertEquals("Is message text: 'The Message'", "The Message", records.iterator().next().value());

        // Check metrics
        TestMetrics metrics = getPrometheusMetrics(URI.create("http://kafka:9404/metrics"));

        BigDecimal value = metrics.getValueSum("strimzi_oauth_http_requests_count", "kind", "introspect", "host", authHostPort, "path", introspectPath, "outcome", "success");
        // Inter-broker connection did some validation, producer and consumer did some
        Assert.assertTrue("strimzi_oauth_http_requests_count for introspect >= 5", value != null && value.intValue() >= 5);

        value = metrics.getValueSum("strimzi_oauth_http_requests_totaltimems", "kind", "introspect", "host", authHostPort, "path", introspectPath, "outcome", "success");
        Assert.assertTrue("strimzi_oauth_http_requests_totaltimems for introspect > 0.0", value.doubleValue() > 0.0);
    }

    static void refreshTokenWithIntrospection() throws Exception {

        System.out.println("==== KeycloakAuthenticationTest :: refreshTokenWithIntrospectionTest ====");

        final String kafkaBootstrap = "kafka:9093";
        final String authHostPort = "keycloak:8080";
        final String realm = "demo";
        final String path = "/realms/" + realm + "/protocol/openid-connect/token";

        // For metrics
        final String introspectPath = "/realms/" + realm + "/protocol/openid-connect/token/introspect";

        final String tokenEndpointUri = "http://" + authHostPort + path;

        final String clientId = "kafka-cli";
        final String username = "alice";
        final String password = "alice-password";

        // First, request access token using client id and secret
        String refreshToken = loginWithUsernameForRefreshToken(URI.create(tokenEndpointUri), username, password, clientId);

        Map<String, String> oauthConfig = new HashMap<>();
        oauthConfig.put(ClientConfig.OAUTH_TOKEN_ENDPOINT_URI, tokenEndpointUri);
        oauthConfig.put(ClientConfig.OAUTH_CLIENT_ID, clientId);
        oauthConfig.put(ClientConfig.OAUTH_REFRESH_TOKEN, refreshToken);
        oauthConfig.put(ClientConfig.OAUTH_USERNAME_CLAIM, "preferred_username");
        oauthConfig.put(ClientConfig.OAUTH_SCOPE, "profile");

        Properties producerProps = buildProducerConfigOAuthBearer(kafkaBootstrap, oauthConfig);
        Producer<String, String> producer = new KafkaProducer<>(producerProps);


        final String topic = "KeycloakAuthenticationTest-refreshTokenWithIntrospectionTest";

        producer.send(new ProducerRecord<>(topic, "The Message")).get();
        System.out.println("Produced The Message");

        Properties consumerProps = buildConsumerConfigOAuthBearer(kafkaBootstrap, oauthConfig);
        Consumer<String, String> consumer = new KafkaConsumer<>(consumerProps);

        TopicPartition partition = new TopicPartition(topic, 0);
        consumer.assign(singletonList(partition));

        while (consumer.partitionsFor(topic, Duration.ofSeconds(1)).size() == 0) {
            System.out.println("No assignment yet for consumer");
        }
        consumer.seekToBeginning(singletonList(partition));

        ConsumerRecords<String, String> records = poll(consumer);

        Assert.assertEquals("Got message", 1, records.count());
        Assert.assertEquals("Is message text: 'The Message'", "The Message", records.iterator().next().value());

        // Check metrics
        TestMetrics metrics = getPrometheusMetrics(URI.create("http://kafka:9404/metrics"));
        BigDecimal value = metrics.getValueSum("strimzi_oauth_http_requests_count", "kind", "introspect", "host", authHostPort, "path", introspectPath, "outcome", "success");
        // On top of the access token test, producer and consumer together did 4 requests
        Assert.assertTrue("strimzi_oauth_http_requests_count for introspect >= 9", value != null && value.intValue() >= 9);

        value = metrics.getValueSum("strimzi_oauth_http_requests_totaltimems", "kind", "introspect", "host", authHostPort, "path", introspectPath, "outcome", "success");
        Assert.assertTrue("strimzi_oauth_http_requests_totaltimems for introspect > 0.0", value.doubleValue() > 0.0);
    }
}
