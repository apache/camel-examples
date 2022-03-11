Resume API Example: File Set
=========================

This example shows how to use the Resume API for processing a large directory. It uses the file component to read a large directory and subdirectories, consisting of about 500 files. The processing reads a batch of 50 files and then stop. Then, when it restarts, it continues from the last file it processed in the previous execution. The offsets are stored in a Kafka topic. 


*Note*: this demo runs in a container. Although it is possible to run it outside a container, doing so requires additional infrastructure. Therefore, it's not covered by the documentation.

*Note 2*: the code is deliberately slowed down for a better display of the execution.

Building the demo
===

To build the demo and the containers:

```shell
mvn clean package && docker-compose build && 
```

Run
===

To run the demo:

```shell
docker-compose up -d && docker-compose logs --no-log-prefix -f example ; docker-compose down
```
