#!/bin/sh
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

function checkResults() {
  expectedItems=$((ITERATIONS * BATCH_SIZE))
  processedRecords=$(cat ${OUTPUT_DIR}/summary.txt | wc -l)
  repeated=$(cat ${OUTPUT_DIR}/summary.txt | sort | uniq --count --repeated | wc -l)

  echo "###**************************************************************************###"
  echo "Results: repeated items: ${repeated}"
  echo "Results: processed items: ${processedRecords} (expected ${expectedItems})"
  echo "###**************************************************************************###"
  echo "Resume simulation completed"
  echo "###**************************************************************************###"

}

trap checkResults exit SIGINT SIGABRT SIGHUP

echo "The test will process the following directory tree:"

mkdir -p ${DATA_DIR}

ITERATIONS=${1:-5}
BATCH_SIZE=${2:-100}
FILE_COUNT=${3:-100}
MAX_IDLE=5

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
  find ${DATA_DIR}/${i} -type f | pv -q -L 1014

  java -Dinput.dir=${DATA_DIR} \
    -Doutput.dir=${OUTPUT_DIR} \
    -Dresume.type=kafka \
    -Dresume.type.kafka.topic=dir-offsets \
    -Dbootstrap.address=kafka:9092 \
    -jar /deployments/example.jar \
    -di ${MAX_IDLE}
    echo "********************************************************************************"
    echo "Finished the iteration ${i}"
    echo "********************************************************************************"
    sleep 2s

    if [[ -f ${OUTPUT_DIR}/summary.txt ]] ; then
      processedRecords=$(cat ${OUTPUT_DIR}/summary.txt | wc -l)
      echo "Processed ${processedRecords} so far"
    fi


done



exit 0
