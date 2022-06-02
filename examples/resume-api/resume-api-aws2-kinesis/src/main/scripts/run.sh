#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
ITERATIONS=${1:-5}
BATCH_SIZE=${2:-50}

echo "Loading data into Kinesis"
  java -Dresume.type=kafka \
    -Dresume.type.kafka.topic=aws-kinesis-offsets \
    -Dbootstrap.address=kafka:9092 \
    -Dresume.action=load \
    -Daws.kinesis.streamName=sample-stream \
    -Daws-service.kinesis.instance.type=remote \
    -Daws.host=kinesis:4566 \
    -Daws.access.key=accesskey \
    -Daws.secret.key=secretkey \
    -Daws.cborEnabled=false \
    -jar /deployments/example.jar
echo "Done loading"
sleep 10s

for i in $(seq 0 ${ITERATIONS}) ; do
  echo "********************************************************************************"
  echo "Running the iteration ${i} of ${ITERATIONS} with a batch of ${BATCH_SIZE} offsets"
  echo "********************************************************************************"
  java -Dresume.type=kafka \
           -Dresume.type.kafka.topic=aws-kinesis-offsets \
           -Dbootstrap.address=kafka:9092 \
           -Daws.kinesis.streamName=sample-stream \
           -Daws-service.kinesis.instance.type=remote \
           -Daws.host=kinesis:4566 \
           -Daws.access.key=accesskey \
           -Daws.secret.key=secretkey \
           -Daws.cborEnabled=false \
           -Dbatch.size=${BATCH_SIZE} \
           -jar /deployments/example.jar
    echo "********************************************************************************"
    echo "Finished the iteration ${i}"
    echo "********************************************************************************"
    sleep 2s
done

echo "###**************************************************************************###"
echo "Resume simulation completed"
echo "###**************************************************************************###"
exit 0
