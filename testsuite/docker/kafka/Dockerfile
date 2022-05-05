FROM quay.io/strimzi/kafka:latest-kafka-3.1.0

USER root
RUN rm -rf /opt/kafka/libs/bcpkix* /opt/kafka/libs/bcprov* /opt/kafka/libs/keycloak*
USER kafka
