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

echo "Creating data file"
for i in $(seq -f '%010g' 1 100000) ; do
  echo $i >> ${DATA_DIR}/data.txt ;
done
echo "Done"
sleep 10s

for i in $(seq 0 ${ITERATIONS}) ; do
  echo "********************************************************************************"
  echo "Running the iteration ${i} of ${ITERATIONS} with a batch of ${BATCH_SIZE} files"
  echo "********************************************************************************"
  java -Dinput.dir=${DATA_DIR} \
    -Doutput.dir=/tmp/out \
    -Dinput.file=${DATA_FILE} \
    -Dresume.type=kafka \
    -Dresume.type.kafka.topic=file-offsets \
    -Dbootstrap.address=kafka:9092 \
    -Dresume.batch.size=${BATCH_SIZE} \
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
