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

# Unit tests of the policies, run with:  opa test /policies  (see the README)
package camel.spiffe_test

import data.camel.spiffe.backend
import data.camel.spiffe.inventory

frontend := "spiffe://example.org/frontend"

auditor := "spiffe://example.org/auditor"

backend_id := "spiffe://example.org/backend"

test_frontend_may_read_the_orders if {
	backend.allow with input as {"headers": {"CamelSpiffeSpiffeId": frontend}, "routeId": "orders"}
}

test_frontend_may_not_read_the_audit_trail if {
	not backend.allow with input as {"headers": {"CamelSpiffeSpiffeId": frontend}, "routeId": "audit"}
}

test_auditor_may_read_the_audit_trail if {
	backend.allow with input as {"headers": {"CamelSpiffeSpiffeId": auditor}, "routeId": "audit"}
}

test_auditor_may_not_read_the_orders if {
	not backend.allow with input as {"headers": {"CamelSpiffeSpiffeId": auditor}, "routeId": "orders"}
}

test_an_unknown_route_is_denied if {
	not backend.allow with input as {"headers": {"CamelSpiffeSpiffeId": frontend}, "routeId": "something-else"}
}

test_a_caller_without_identity_is_denied if {
	not backend.allow with input as {"headers": {}, "routeId": "orders"}
}

test_backend_may_ask_the_stock_on_behalf_of_the_frontend if {
	inventory.allow with input as {"headers": {"CamelSpiffeSpiffeId": backend_id, "X-On-Behalf-Of": frontend}, "routeId": "stock"}
}

test_backend_may_not_ask_the_stock_on_behalf_of_the_auditor if {
	not inventory.allow with input as {"headers": {"CamelSpiffeSpiffeId": backend_id, "X-On-Behalf-Of": auditor}, "routeId": "stock"}
}

test_backend_may_not_ask_the_stock_on_behalf_of_nobody if {
	not inventory.allow with input as {"headers": {"CamelSpiffeSpiffeId": backend_id}, "routeId": "stock"}
}

test_frontend_may_not_ask_the_inventory_directly if {
	not inventory.allow with input as {"headers": {"CamelSpiffeSpiffeId": frontend, "X-On-Behalf-Of": frontend}, "routeId": "stock"}
}
