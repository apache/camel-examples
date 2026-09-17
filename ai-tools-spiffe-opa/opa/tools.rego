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

# The authorization policy for the AI assistant's tools, evaluated in-process by the camel-opa component from the
# WebAssembly bundle built out of this file (build-policy.sh runs "opa build -t wasm -e ai/tools/allow"). It decides
# whether the authenticated caller may invoke the tool the language model chose. Camel sends an input document such as
#
#   {
#     "properties": {"subject": "spiffe://example.org/support-console", "tool": "refundOrder"},
#     "headers": {"orderId": "1002", "amount": "50"}
#   }
#
# where "subject" is the SPIFFE ID that the assistant obtained by validating the caller's JWT-SVID (not something the
# model or the prompt can set), "tool" is the tool the model chose to call, and the headers are the tool arguments the
# model filled in. OPA answers the boolean published at ai/tools/allow.
package ai.tools

default allow := false

# the authenticated caller, established by the SPIFFE JWT-SVID the assistant validated before running the model
subject := input.properties.subject

# the tool the model is trying to call
tool := input.properties.tool

# which tools each caller is trusted with. The public chatbot may only look things up; the internal support console
# may also see customer data and issue refunds. A prompt-injected model can still ask for refundOrder on behalf of the
# public chatbot, but it is not on that caller's list, so the tool never runs.
tools := {
	"spiffe://example.org/public-chatbot": {"getOrderStatus"},
	"spiffe://example.org/support-console": {"getOrderStatus", "lookupCustomer", "refundOrder"},
}

# a caller may invoke a tool that is on its list, with refunds carrying one extra condition (below)
allow if {
	tool in tools[subject]
	tool != "refundOrder"
}

# refunds are also capped: the amount must not exceed data.limits.refund_max, which travels inside the bundle as
# data (opa/data.json) rather than being hard-coded in the policy
allow if {
	tool == "refundOrder"
	"refundOrder" in tools[subject]
	to_number(input.headers.amount) <= data.limits.refund_max
}
