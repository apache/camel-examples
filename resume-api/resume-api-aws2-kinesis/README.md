Resume API Example: AWS Kinesis
=========================

This example shows how to use the Resume API for consuming AWS Kinesis data streams. 

It uses LocalStack to simulate the AWS Kinesis instance. 

First the demo load 500 records into the data stream. Then, it consumes then in batches of 50 messages. It starts each batch at the point of last consumption. The offsets are stored in a Kafka topic.

*Note*: this demo runs in a container. Although it is possible to run it outside a container, doing so requires additional infrastructure. Therefore, it's not extensively covered by the documentation.

Building the demo
===

To build the demo and the containers:

```shell
mvn clean package && docker compose build
```

Run
===

To run the demo:

```shell
docker compose up -d && docker compose logs --no-log-prefix -f example ; docker compose down
```

Advanced / Manual
===

Launch LocalStack
====

```
docker run --rm -e SERVICES=kinesis -e DEBUG=1 -e LS_LOG=trace -it -p 4566:4566 localstack/localstack:latest
```

Data Load
======

To load the data run: 

```shell
  java -Dresume.type=kafka \
    -Dresume.type.kafka.topic=aws-kinesis-offsets \
    -Dresume.action=load \
    -Daws.kinesis.streamName=sample-stream \
    -Daws-service.kinesis.instance.type=remote \
    -Daws.host=kinesis:4566 \
    -Daws.access.key=accesskey \
    -Daws.secret.key=secretkey \
    -Daws.cborEnabled=false \
    -jar /deployments/example.jar
```

Run the Example
======

```shell
java -Dresume.type=kafka \
           -Dresume.type.kafka.topic=aws-kinesis-offsets \
           -Dbootstrap.address=REPLACE-WITH-KAFKA-HOST:9092 \
           -Daws.kinesis.streamName=sample-stream \
           -Daws-service.kinesis.instance.type=remote \
           -Daws.host=REPLACE-WITH-KINESIS-HOST:4566 \
           -Daws.access.key=accesskey \
           -Daws.secret.key=secretkey \
           -Daws.cborEnabled=false \
           -Dbatch.size=50 \
           -jar /deployments/example.jar
```
