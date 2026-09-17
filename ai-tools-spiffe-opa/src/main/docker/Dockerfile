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

FROM eclipse-temurin:21-jre

# each application of the example runs as its own Unix user: the SPIRE agent maps the uid to a SPIFFE ID
RUN useradd --uid 2001 --user-group --no-create-home assistant \
    && useradd --uid 2002 --user-group --no-create-home public-chatbot \
    && useradd --uid 2003 --user-group --no-create-home support-console

COPY target/lib /deployments/lib
COPY target/camel-example-ai-tools-spiffe-opa-*.jar /deployments/lib/

WORKDIR /deployments
ENTRYPOINT ["java", "-cp", "/deployments/lib/*"]
# the main class to run: compose.yaml overrides it for the two clients
CMD ["org.apache.camel.example.aitools.assistant.AssistantApplication"]
