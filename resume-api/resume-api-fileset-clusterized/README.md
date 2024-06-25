Resume API Example: Clusterized File Set
=========================

This example shows how to use the Resume API for processing a large directory in clusterized mode. It uses the master component along with the file component to read a large directory and subdirectories, consisting of about 500 files. The processing reads a batch of 25 files and then stop. At this point, then the secondary node starts processing. Then, when it restarts, the primary continues from the last file it processed in the previous execution. The offsets are stored in a Kafka topic.

For simplicity, this demo reuses the same Zookeeper instance as the Kafka node. For production usage, you are advised to consult Kafka, ZooKeeper and/or your vendor documentation for the best practices regarding reusing the ZooKeeper instance.


```
        ┌───────────────┐                            ┌──────────────┐
        │               │                            │              │
        │     Kafka     │                            │   Zookeeper  │
        │               │                            │              │
        └─────▲────▲────┘                            └─────▲────▲───┘
              │    │                                       │    │
              │    │                                       │    │
              │    │         ┌───────────────┐             │    │
              │    │         │               │             │    │
              │    └─────────┤   Camel 1     ├─────────────┘    │
              │              │               │                  │
              │              └───────────────┘                  │
              │                                                 │
              │              ┌───────────────┐                  │
              │              │               │                  │
              └──────────────┤  Camel 2      ├──────────────────┘
                             │               │
                             └───────────────┘
```


*Note*: this demo runs in a container. Although it is possible to run it outside a container, doing so requires additional infrastructure. Therefore, it's not covered by the documentation.

*Note 2*: the code is deliberately slowed down for a better display of the execution.

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
docker compose up -d && docker compose logs -f example-1 example-2 ; docker compose down
```




