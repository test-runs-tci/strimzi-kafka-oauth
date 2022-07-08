#!/bin/bash
set -e

source functions.sh

URI=${KEYCLOAK_URI}
if [ "" == "${URI}" ]; then
    URI="http://${KEYCLOAK_HOST:-keycloak}:8080/admin"
fi

wait_for_url $URI "Waiting for Keycloak to start"


./simple_kafka_config.sh | tee /tmp/strimzi.properties


# Add 'admin' user
KAFKA_DEBUG= /opt/kafka/bin/kafka-configs.sh --zookeeper zookeeper:2181 --alter --add-config 'SCRAM-SHA-512=[password=admin-secret]' --entity-type users --entity-name admin

# Add 'alice' user
KAFKA_DEBUG= /opt/kafka/bin/kafka-configs.sh --zookeeper zookeeper:2181 --alter --add-config 'SCRAM-SHA-512=[password=alice-secret]' --entity-type users --entity-name alice


# set log dir to writable directory
if [ "$LOG_DIR" == "" ]; then
  export LOG_DIR=/tmp/logs
fi

# set log4j properties file to custom one
if [ "$KAFKA_LOG4J_OPTS" == "" ]; then
  export KAFKA_LOG4J_OPTS="-Dlog4j.configuration=file:/opt/kafka/config/strimzi/log4j.properties"
fi
echo "KAFKA_LOG4J_OPTS=$KAFKA_LOG4J_OPTS"

# add extra jars to classpath
export CLASSPATH="/opt/kafka/libs/strimzi/*:$CLASSPATH"
echo "CLASSPATH=$CLASSPATH"

# Prometheus JMX agent config
if [ "$PROMETHEUS_AGENT_CONFIG" == "" ]; then

  if [ "$PROMETHEUS_AGENT_VERSION" == "" ]; then
    PROMETHEUS_AGENT_VERSION=$(ls /opt/kafka/libs/jmx_prometheus* | sed -E -n 's/.*([0-9]+\.[0-9]+\.[0-9]+).*$/\1/p')
  fi

  export PROMETHEUS_AGENT_CONFIG="-javaagent:/opt/kafka/libs/jmx_prometheus_javaagent-$PROMETHEUS_AGENT_VERSION.jar=9404:/opt/kafka/config/strimzi/metrics-config.yml"
fi
echo "PROMETHEUS_AGENT_CONFIG=$PROMETHEUS_AGENT_CONFIG"

export KAFKA_OPTS="$PROMETHEUS_AGENT_CONFIG $KAFKA_OPTS"
echo "KAFKA_OPTS=$KAFKA_OPTS"

exec /opt/kafka/bin/kafka-server-start.sh /tmp/strimzi.properties
