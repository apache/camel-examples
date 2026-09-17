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

# Unit tests for the tool authorization policy. Run them with the Open Policy Agent CLI:
#
#   docker run --rm -v "$PWD/opa:/policies:ro,z" openpolicyagent/opa:1.9.0-static test /policies -v
#
# They exercise the policy against opa/data.json, the same data the WebAssembly bundle carries.
package ai.tools_test

import data.ai.tools

PUBLIC := "spiffe://example.org/public-chatbot"

CONSOLE := "spiffe://example.org/support-console"

# the public chatbot may read an order's status
test_public_chatbot_may_get_order_status if {
	tools.allow with input as {"properties": {"subject": PUBLIC, "tool": "getOrderStatus"}, "headers": {"orderId": "1002"}}
}

# ...but not look up customer data
test_public_chatbot_may_not_look_up_customer if {
	not tools.allow with input as {"properties": {"subject": PUBLIC, "tool": "lookupCustomer"}, "headers": {"orderId": "1002"}}
}

# a prompt-injected refund on behalf of the public chatbot is denied, whatever the amount
test_public_chatbot_may_not_refund if {
	not tools.allow with input as {"properties": {"subject": PUBLIC, "tool": "refundOrder"}, "headers": {"orderId": "1002", "amount": "5"}}
}

# the support console may look up customer data
test_support_console_may_look_up_customer if {
	tools.allow with input as {"properties": {"subject": CONSOLE, "tool": "lookupCustomer"}, "headers": {"orderId": "1002"}}
}

# ...and refund within the cap
test_support_console_may_refund_within_cap if {
	tools.allow with input as {"properties": {"subject": CONSOLE, "tool": "refundOrder"}, "headers": {"orderId": "1002", "amount": "50"}}
}

# ...but not above it
test_support_console_may_not_refund_above_cap if {
	not tools.allow with input as {"properties": {"subject": CONSOLE, "tool": "refundOrder"}, "headers": {"orderId": "1002", "amount": "500"}}
}

# an unknown caller is denied everything (default deny)
test_unknown_caller_is_denied if {
	not tools.allow with input as {"properties": {"subject": "spiffe://example.org/intruder", "tool": "getOrderStatus"}, "headers": {"orderId": "1002"}}
}
