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

mkdir -p ${DATA_DIR}

ITERATIONS=${1:-5}
BATCH_SIZE=${2:-50}
FILE_COUNT=${3:-100}

for i in $(seq 0 ${ITERATIONS}) ; do
  mkdir -p ${DATA_DIR}/${i}

  echo "********************************************************************************"
  echo "Running the iteration ${i} of ${ITERATIONS} with a batch of ${BATCH_SIZE} files"
  echo "********************************************************************************"

  echo "Appending ${FILE_COUNT} files to the data directory (files from the previous execution remain there)"
  for file in $(seq 1 100) ; do
    echo ${RANDOM} > ${DATA_DIR}/${i}/${file}
  done

  echo "Only the following files should processed in this execution:"
  tree ${DATA_DIR}/${i} | pv -q -L 1014

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
