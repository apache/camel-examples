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

#Requires -Version 5.1
param([string]$EnvFile = "./camel-agent-routing.env")

$ErrorActionPreference = 'Stop'
if ($env:OS -ne 'Windows_NT') {
    throw 'Use start.sh on Linux or macOS.'
}

if (Test-Path -LiteralPath $EnvFile -PathType Leaf) {
    foreach ($line in Get-Content -LiteralPath $EnvFile) {
        if ($line -match '^\s*(#|$)') { continue }
        if ($line -notmatch '^\s*(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
            throw 'Use KEY=value entries and whole-line comments in the environment file.'
        }
        $name, $value = $Matches[1], $Matches[2].Trim()
        if ($value -match "^'(.*)'$") {
            $value = $Matches[1]
        } else {
            if ($value -match '^"(.*)"$') { $value = $Matches[1] }
            # Expand references such as DECISION_API_KEY=${JEV_API_KEY}, without evaluating code.
            $value = [regex]::Replace($value, '\$\{([A-Za-z_][A-Za-z0-9_]*)\}', {
                param($reference)
                $replacement = [Environment]::GetEnvironmentVariable($reference.Groups[1].Value)
                if ($null -eq $replacement) { throw 'An environment-file reference is not defined.' }
                return $replacement
            })
        }
        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
    }
} elseif ($PSBoundParameters.ContainsKey('EnvFile')) {
    throw "Environment file not found: $EnvFile"
}
if (-not $env:OPENAI_API_KEY) {
    throw 'Set OPENAI_API_KEY in the environment or shared environment file.'
}
$camel = (Get-Command camel.cmd -CommandType Application -ErrorAction Stop).Source

foreach ($port in 8080..8084) {
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        try { $client.Connect('127.0.0.1', $port) } catch [System.Net.Sockets.SocketException] { }
        if ($client.Connected) { throw "Port $port is already in use. Stop the existing application first." }
    } finally { $client.Dispose() }
}

$logDir = Join-Path $PSScriptRoot 'target/run-logs'
$resources = Join-Path $PSScriptRoot 'src/main/resources'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$applications = New-Object System.Collections.ArrayList

function Start-Integration([string]$Role, [int]$Port, [string]$Extra = '') {
    $log = Join-Path $logDir "$Role.log"
    $errorLog = Join-Path $logDir "$Role.err.log"
    Write-Host "Starting $Role on port $Port (logs: $log and $errorLog)"
    # cmd.exe runs the Camel JBang launcher; /s /c requires the outer pair of quotes.
    $command = '/d /s /c ""{0}" run routes/{1}.yaml --property=role={1} --property=port={2} {3}"' -f $camel, $Role, $Port, $Extra
    $process = Start-Process -FilePath $env:ComSpec -ArgumentList $command -WorkingDirectory $resources `
        -NoNewWindow -RedirectStandardOutput $log -RedirectStandardError $errorLog -PassThru
    [void]$applications.Add(@{ Role = $Role; Process = $process })
    $deadline = (Get-Date).AddMinutes(5)
    while (-not (Select-String -LiteralPath $log -Pattern 'Apache Camel .* started in ' -Quiet)) {
        if ($process.HasExited -or (Get-Date) -ge $deadline) {
            throw "$Role did not start within five minutes. See $log and $errorLog"
        }
        Start-Sleep -Seconds 1
    }
}

try {
    Start-Integration reservation 8081
    Start-Integration weather 8082
    Start-Integration cost 8083
    Start-Integration general 8084
    Start-Integration coordinator 8080 '../java/org/apache/camel/example/routing/TripSupport.java --dep=camel-semantic,camel-typesafe-ai'
    Write-Host 'Ready: http://127.0.0.1:8080/trip - press Ctrl+C to stop all five applications.'
    while ($true) {
        foreach ($application in $applications) {
            if ($application.Process.HasExited) {
                throw "$($application.Role) stopped. See its logs in $logDir"
            }
        }
        Start-Sleep -Seconds 1
    }
} finally {
    Write-Host 'Stopping the Camel applications started by this script...'
    foreach ($application in $applications) {
        if (-not $application.Process.HasExited) {
            # Terminate this launcher's process tree, including JBang's child JVMs.
            try {
                & taskkill.exe /PID $application.Process.Id /T /F 2>$null | Out-Null
            } catch {
                if (-not $application.Process.HasExited) {
                    Write-Warning "Could not stop $($application.Role) (PID $($application.Process.Id))."
                }
            }
        }
    }
}
