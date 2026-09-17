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

# The official SPIRE images are built from scratch and have no shell. This image copies their static binaries
# into a small Alpine image, so that entrypoint.sh can bootstrap a complete SPIRE deployment for the example:
# a server, an agent and the registration entries of the workloads.
FROM ghcr.io/spiffe/spire-server:1.15.3 AS spire-server
FROM ghcr.io/spiffe/spire-agent:1.15.3 AS spire-agent

FROM alpine:3.22

COPY --from=spire-server /opt/spire/bin/spire-server /opt/spire/bin/spire-server
COPY --from=spire-agent /opt/spire/bin/spire-agent /opt/spire/bin/spire-agent
COPY server.conf agent.conf /opt/spire/conf/
COPY entrypoint.sh /opt/spire/entrypoint.sh

RUN chmod 0755 /opt/spire/entrypoint.sh \
    && mkdir -p /opt/spire/data/server /opt/spire/data/agent /run/spire/sockets /tmp/spire-server/private

ENTRYPOINT ["/opt/spire/entrypoint.sh"]
