#!/bin/bash
for (( c=1; c<=$2; c++ ))
do  
  kafkacat -P -b localhost:9092 -t $1 $3
done
