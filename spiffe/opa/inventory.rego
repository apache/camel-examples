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

# The authorization policy of the inventory, evaluated by Open Policy Agent as camel/spiffe/inventory/allow.
package camel.spiffe.inventory

default allow := false

# HTTP header names are case-insensitive, so look them up in lower case
headers := {lower(name): value | some name, value in input.headers}

# Only the backend may ask for the stock levels, and only on behalf of a caller that may read the orders.
# The permissions of the backend are data of the same OPA server, so the policy can refer to them.
allow if {
	headers.camelspiffespiffeid == "spiffe://example.org/backend"
	headers["x-on-behalf-of"] in data.camel.spiffe.backend.permissions.orders
}
