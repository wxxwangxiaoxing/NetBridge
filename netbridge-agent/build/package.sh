#!/usr/bin/env bash
set -euo pipefail

go build -o build/netbridge-agent ./cmd/agent
