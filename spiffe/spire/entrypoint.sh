#!/bin/sh
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

# Bootstraps a complete (single node) SPIRE deployment for the example:
#  1. starts the SPIRE server
#  2. registers the workloads, mapping the Unix user id of each Camel application to its SPIFFE ID
#  3. generates a join token and starts the SPIRE agent with it, which then serves the SPIFFE Workload API
set -e

SPIRE_BIN=/opt/spire/bin
SERVER_SOCKET=/tmp/spire-server/private/api.sock
TRUST_DOMAIN=example.org
AGENT_ID="spiffe://${TRUST_DOMAIN}/spire-agent"

# 1. the server issues all the identities of the trust domain
"${SPIRE_BIN}/spire-server" run -config /opt/spire/conf/server.conf &

echo "Waiting for the SPIRE server to be ready..."
until "${SPIRE_BIN}/spire-server" healthcheck -socketPath "${SERVER_SOCKET}" > /dev/null 2>&1; do
    sleep 1
done

# the agent verifies the server with the CA bundle of the trust domain
"${SPIRE_BIN}/spire-server" bundle show -socketPath "${SERVER_SOCKET}" > /opt/spire/data/bootstrap.crt

# 2. a registration entry tells SPIRE which workload (selectors) gets which identity (SPIFFE ID).
# The Camel applications of this example each run as a different Unix user, so the uid is the selector.
register() {
    echo "Registering spiffe://${TRUST_DOMAIN}/$1 for the workload running with uid $2"
    "${SPIRE_BIN}/spire-server" entry create -socketPath "${SERVER_SOCKET}" \
        -parentID "${AGENT_ID}" \
        -spiffeID "spiffe://${TRUST_DOMAIN}/$1" \
        -selector "unix:uid:$2"
}
register frontend 1001
register backend 1002
register auditor 1003
register inventory 1004

# 3. the agent attests to the server with a one-time join token (the -spiffeID option also gives the agent
# the alias AGENT_ID, which the entries above use as parent ID)
TOKEN=$("${SPIRE_BIN}/spire-server" token generate -socketPath "${SERVER_SOCKET}" -spiffeID "${AGENT_ID}" \
    | awk '/^Token:/ { print $2 }')

exec "${SPIRE_BIN}/spire-agent" run -config /opt/spire/conf/agent.conf -joinToken "${TOKEN}"
