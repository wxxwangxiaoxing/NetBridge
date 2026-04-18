#!/usr/bin/env bash
set -euo pipefail

INSTALL_DIR="${INSTALL_DIR:-/opt/netbridge-agent}"
SERVICE_NAME="${SERVICE_NAME:-netbridge-agent}"
SYSTEMD_UNIT_PATH="/etc/systemd/system/${SERVICE_NAME}.service"

if command -v systemctl >/dev/null 2>&1; then
  systemctl stop "${SERVICE_NAME}" 2>/dev/null || true
  systemctl disable "${SERVICE_NAME}" 2>/dev/null || true
  rm -f "${SYSTEMD_UNIT_PATH}"
  systemctl daemon-reload
fi

rm -rf "${INSTALL_DIR}"
echo "uninstalled ${SERVICE_NAME}"
