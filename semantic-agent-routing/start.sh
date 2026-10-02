#!/usr/bin/env bash
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

set -euo pipefail

if (( $# > 1 )); then
    echo "Usage: $0 [environment-file]" >&2
    exit 1
fi

example_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
env_file=${1:-./camel-agent-routing.env}
if [[ -f "$env_file" ]]; then
    set -a
    # Load the private file without passing credentials on the command line.
    source "$env_file"
    set +a
elif (( $# == 1 )); then
    echo "Environment file not found: $env_file" >&2
    exit 1
fi
: "${OPENAI_API_KEY:?Set OPENAI_API_KEY in the environment or shared environment file}"
command -v camel >/dev/null || { echo 'Install Camel JBang first.' >&2; exit 1; }

for port in 8080 8081 8082 8083 8084; do
    if (: >"/dev/tcp/127.0.0.1/$port") 2>/dev/null; then
        echo "Port $port is already in use. Stop the existing application first." >&2
        exit 1
    fi
done

log_dir="$example_dir/target/run-logs"
mkdir -p "$log_dir"
cd "$example_dir/src/main/resources"
pids=()
roles=()
# Give each Camel command its own process group, including JBang's child JVMs.
set -m
cleanup() {
    trap - EXIT INT TERM
    echo 'Stopping the Camel applications started by this script...'
    for pid in "${pids[@]}"; do
        kill -TERM -- "-$pid" 2>/dev/null || true
    done
    wait || true
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

start() {
    local role=$1 port=$2 pid deadline
    shift 2
    echo "Starting $role on port $port (log: $log_dir/$role.log)"
    camel run "routes/$role.yaml" --property="role=$role" --property="port=$port" \
        "$@" >"$log_dir/$role.log" 2>&1 < /dev/null &
    pid=$!
    pids+=("$pid")
    roles+=("$role")
    deadline=$((SECONDS + 300))
    until grep -q 'Apache Camel .* started in ' "$log_dir/$role.log"; do
        if ! kill -0 "$pid" 2>/dev/null || (( SECONDS >= deadline )); then
            echo "$role did not start within five minutes. See $log_dir/$role.log" >&2
            exit 1
        fi
        sleep 1
    done
}

start reservation 8081
start weather 8082
start cost 8083
start general 8084
start coordinator 8080 ../java/org/apache/camel/example/routing/TripSupport.java \
    --dep=camel-semantic,camel-typesafe-ai
echo 'Ready: http://127.0.0.1:8080/trip — press Ctrl+C to stop all five applications.'

while true; do
    for i in "${!pids[@]}"; do
        if ! kill -0 "${pids[$i]}" 2>/dev/null; then
            echo "${roles[$i]} stopped. See $log_dir/${roles[$i]}.log" >&2
            exit 1
        fi
    done
    sleep 1
done
