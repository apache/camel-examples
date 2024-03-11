Resume API Example: File Set with Write Ahead Log
=========================

This example shows how to use the Resume API for processing a large directory using a the write ahead log. It uses the file component to read a large directory and subdirectories, consisting of about 500 files. The processing reads a batch of 50 files and then stop. Then, when it restarts, it continues from the last file it processed in the previous execution. The offsets are stored in a Kafka topic.


*Note*: this demo runs in a container. Although it is possible to run it outside a container, doing so requires additional infrastructure. Therefore, it's not covered by the documentation.

*Note 2*: the code is deliberately slowed down for a better display of the execution.

Building the demo
===

To build the demo and the containers:

```shell
mvn clean package && docker-compose build
```

Run
===

This demo runs manually. First, start the demo using the command:

```shell
docker-compose up -d && docker exec -it resume-api-fileset-wal-example-1 /bin/bash ; docker-compose down
```

This will take you to another shell. Open another window or terminal and leave that shell open for now. 

In the new terminal, connect to the Kafka topic used for the offsets:

```shell
docker exec -it resume-api-fileset-wal-kafka-1 ./bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic dir-offsets
```

Go back to the first terminal and run the demo:

```shell
./run.sh
```

You'll need to press Enter after every iteration.

Open one more terminal to simulate outages in the Kafka instance used for the offsets. 

For instance, use the following command to disconnect that instance from the network:

```shell
docker network disconnect --force resume-api-fileset-wal_default resume-api-fileset-wal-kafka-1
```

Kafka can be reconnected to the network using the following command: 

```shell
docker network connect --alias kafka resume-api-fileset-wal_default resume-api-fileset-wal-kafka-1
```

While running the demo, note how the application performs the recovery prior to starting the execution. 


*Note*: it assumes docker-compose will create the containers using its default name pattern. If that's not the case, 
please adjust the names accordingly.



