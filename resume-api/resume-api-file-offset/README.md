Resume API Example: File Offsets
=========================

This example shows how to use the Resume API for processing a large file. It uses the file component to read a large text file. The processing reads 30 lines of the file and then stop. Then, when it restarts, it continues from the last line it processed in the previous execution. The offsets are stored in a Kafka topic.


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
docker compose up -d && docker compose logs --no-log-prefix -f example ; docker compose down
```

Advanced / Manual
===

Prepare the data sets
====

To create a large file for processing, run:

Large file
======
```shell
cat /dev/null > data.txt ; for i in $(seq -f '%010g' 1 10000000) ; do echo $i >> data.txt ; done
```

Verify command
```
cat summary.txt | sort | uniq --count --repeated | wc -l
```


Large directories
==== 

To create a large directory tree for processing, run:

**Very Small (500 files)**

```shell
mkdir very-small && cd very-small
for dir in $(seq 1 5) ; do mkdir $dir && (cd $dir && (for file in $(seq 1 100) ; do echo $RANDOM > $file ; done) ; cd ..) ; done
```

**Small (25000 files)**

```shell
mkdir small && cd small
for dir in $(seq 1 5) ; do mkdir $dir && (cd $dir && (for file in $(seq 1 5000) ; do echo $RANDOM > $file ; done) ; cd ..) ; done
```

**Medium (50000 files)**

```shell
mkdir medium && cd medium
for dir in $(seq 1 5) ; do mkdir $dir && (cd $dir && (for file in $(seq 1 10000) ; do echo $RANDOM > $file ; done) ; cd ..) ; done
```

**Large (100000 files)**

```shell
mkdir large && cd large
for dir in $(seq 1 10) ; do mkdir $dir && (cd $dir && (for file in $(seq 1 10000) ; do echo $RANDOM > $file ; done) ; cd ..) ; done
```

**Very Large (2000000 files)**

```shell
mkdir very-large && cd very-large
for dir in $(seq 1 2) ; do mkdir $dir && (cd $dir && (for file in $(seq 1 1000000) ; do echo $RANDOM > $file ; done) ; cd ..) ; done
```
