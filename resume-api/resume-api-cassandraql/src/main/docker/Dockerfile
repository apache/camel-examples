# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
FROM fedora:35 as resume-api-file-offset
LABEL maintainer="orpiske@apache.org"


ENV DATA_DIR /data/source
ENV DATA_FILE data.txt
ENV DEPLOYMENT_DIR /deployments

RUN dnf install -y java-11-openjdk-headless tree && dnf clean all
ENV JAVA_HOME /etc/alternatives/jre

COPY target/resume-api-*with-dependencies.jar ${DEPLOYMENT_DIR}/example.jar
COPY src/main/scripts/run.sh ${DEPLOYMENT_DIR}/run.sh

RUN mkdir -p ${DATA_DIR} && \
    cd ${DATA_DIR} && \
    chmod +x ${DEPLOYMENT_DIR}/*.sh
WORKDIR ${DEPLOYMENT_DIR}
CMD [ "sh", "-c", "${DEPLOYMENT_DIR}/run.sh" ]
