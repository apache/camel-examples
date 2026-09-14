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

# The authorization policy of the backend, evaluated by Open Policy Agent. Camel sends OPA an input document
# such as
#
#   {"headers": {"CamelSpiffeSpiffeId": "spiffe://example.org/frontend"}, "routeId": "orders", "exchangeId": "..."}
#
# and reads the boolean answer of the "allow" rule, published by OPA as camel/spiffe/backend/allow.
package camel.spiffe.backend

default allow := false

# which SPIFFE IDs may call which route of the backend
permissions := {
	"orders": {"spiffe://example.org/frontend"},
	"audit": {"spiffe://example.org/auditor"},
}

allow if {
	input.headers.CamelSpiffeSpiffeId in permissions[input.routeId]
}
