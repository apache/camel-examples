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
FROM fedora:35 as resume-api-fileset-wal
LABEL maintainer="orpiske@apache.org"

RUN dnf install -y java-11-openjdk-headless java-11-openjdk-devel tree pv && dnf clean all
COPY target/resume-api-*with-dependencies.jar /deployments/example.jar
COPY src/main/scripts/run.sh /deployments/run.sh
COPY src/main/scripts/noop.sh /deployments/noop.sh

ENV JAVA_HOME /etc/alternatives/jre
ENV DATA_DIR /data
ENV INPUT_DIR ${DATA_DIR}/source
ENV OUTPUT_DIR ${DATA_DIR}/output

RUN chmod +x /deployments/*.sh
WORKDIR /deployments/
CMD [ "sh", "-c", "/deployments/noop.sh" ]
