#!/bin/bash

# Create topics with multiple partitions for load balancing
kafka-topics.sh --create --topic iso8583-requests --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic iso8583-responses --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

echo "Kafka topics created with 3 partitions each for load balancing"