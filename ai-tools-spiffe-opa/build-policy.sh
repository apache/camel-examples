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

# Compiles the Rego policy in opa/ into the WebAssembly bundle that the camel-opa component evaluates in-process
# (src/main/resources/opa/tools-bundle.tar.gz). The bundle is checked in so that the build and the tests need no OPA
# toolchain; run this script and rebuild the module only when opa/tools.rego or opa/data.json change.
#
# It uses the Open Policy Agent CLI from its container image, so only Docker (or Podman) is required. "opa build -e"
# fixes the entrypoint the bundle exposes; it must match the entrypoint the endpoint asks for in ToolAuthorizationPolicy.
set -e

cd "$(dirname "$0")"

OPA_IMAGE="${OPA_IMAGE:-openpolicyagent/opa:1.9.0-static}"
CONTAINER="${CONTAINER_ENGINE:-docker}"

mkdir -p src/main/resources/opa

# the :z flag lets the container read the directory on hosts with SELinux (Fedora, RHEL)
"${CONTAINER}" run --rm -v "$PWD:/work:z" -w /work "${OPA_IMAGE}" \
    build -t wasm -e ai/tools/allow \
    -o src/main/resources/opa/tools-bundle.tar.gz \
    opa/tools.rego opa/data.json

echo "Wrote src/main/resources/opa/tools-bundle.tar.gz"
