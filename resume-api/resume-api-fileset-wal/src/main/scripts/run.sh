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
  echo "Results: processed items: ${processedRecords} (expected at least ${expectedItems})"
  echo "###**************************************************************************###"
  echo "Resume simulation completed"
  echo "###**************************************************************************###"

}

trap checkResults exit SIGINT SIGABRT SIGHUP

echo "The test will process the following directory tree:"

mkdir -p ${INPUT_DIR}

ITERATIONS=${1:-3}
BATCH_SIZE=${2:-20}
FILE_COUNT=${3:-40}
MAX_IDLE=10
PROCESSING_DELAY=${4:-1000}

for i in $(seq 0 ${ITERATIONS}) ; do
  mkdir -p ${INPUT_DIR}/${i}

  echo "********************************************************************************"
  echo "Running the iteration ${i} of ${ITERATIONS} with a batch of ${BATCH_SIZE} files"
  echo "********************************************************************************"

  echo "Appending ${FILE_COUNT} files to the data directory (files from the previous execution remain there)"
  for file in $(seq 1 ${FILE_COUNT}) ; do
    echo ${RANDOM} > ${INPUT_DIR}/${i}/${file}
  done

  echo "Only the following files from the directory ${INPUT_DIR}/${i} should processed in this execution"

  java -Dinput.dir=${INPUT_DIR} \
    -Dwal.log.file=${DATA_DIR}/resume.data \
    -Doutput.dir=${OUTPUT_DIR} \
    -Dprocessing.delay=${PROCESSING_DELAY} \
    -Dresume.type=kafka \
    -Dresume.type.kafka.topic=dir-offsets \
    -Dbootstrap.address=kafka:9092 \
    -jar /deployments/example.jar \
    -di ${MAX_IDLE}
    echo "********************************************************************************"
    echo "Finished the iteration ${i} (press Enter to continue)"
    echo "********************************************************************************"
    read -r

    if [[ -f ${OUTPUT_DIR}/summary.txt ]] ; then
      processedRecords=$(cat ${OUTPUT_DIR}/summary.txt | wc -l)
      echo "Processed ${processedRecords} so far"
    fi


done



exit 0
