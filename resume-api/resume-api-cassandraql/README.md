Resume API Example: Cassandra
=========================

This example shows how to use the Resume API for consuming data from Apache Cassandra.

First the demo load 500 records into the database. Then, it consumes then in batches of 50 messages. It starts each batch at the point of last consumption. The offsets are stored in a Kafka topic.

*Note*: this demo runs in a container. Although it is possible to run it outside a container, doing so requires additional infrastructure. Therefore, it's not extensively covered by the documentation.

Building the demo
===

To build the demo and the containers:

```shell
mvn clean package && docker-compose build
```

Run
===

To run the demo:

```shell
docker-compose up -d && docker-compose logs --no-log-prefix -f example ; docker-compose down
```

Advanced / Manual
===

Launch Cassandra
====

```
docker run -p 9042:9042 -it cassandra:4
```

Data Load
======

To load the data run: 

```shell
  java -Dcassandra.host=cassandra-host.com \
       -Dcassandra.cql3.port=9042 \
       -Dresume.action=load \
       -jar /deployments/example.jar
```

Note: make sure you have copied the jar file generated during the build to `/deployments/example.jar`.

Run the Example
======

```shell
java -Dresume.type=kafka \
           -Dresume.type=kafka \
           -Dresume.type.kafka.topic=cassandra-offsets \
           -Dcassandra.host=cassandra-host.com \
           -Dcassandra.cql3.port=9042 \
           -Dbootstrap.address=kafka-host:9092 \
           -Dbatch.size=50 \
           -jar /deployments/example.jar
```
