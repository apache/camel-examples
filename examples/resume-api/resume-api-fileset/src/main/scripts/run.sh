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
echo "The test will process the following directory tree:"

sleep 2s
tree ${DATA_DIR} | pv -q -L 512
sleep 8s

ITERATIONS=${1:-5}
BATCH_SIZE=${2:-50}

for i in $(seq 0 ${ITERATIONS}) ; do
  echo "********************************************************************************"
  echo "Running the iteration ${i} of ${ITERATIONS} with a batch of ${BATCH_SIZE} files"
  echo "********************************************************************************"
  java -Dinput.dir=${DATA_DIR} \
    -Doutput.dir=/tmp/out \
    -Dresume.type=kafka \
    -Dresume.type.kafka.topic=dir-offsets \
    -Dbootstrap.address=kafka:9092 \
    -jar /deployments/example.jar \
    -dm ${BATCH_SIZE}
    echo "********************************************************************************"
    echo "Finished the iteration ${i}"
    echo "********************************************************************************"
    sleep 2s
done

echo "###**************************************************************************###"
echo "Resume simulation completed"
echo "###**************************************************************************###"
exit 0
