#!/usr/bin/env bash
set -euo pipefail

INSTALL_DIR="${INSTALL_DIR:-/opt/netbridge-agent}"
SERVICE_NAME="${SERVICE_NAME:-netbridge-agent}"
BINARY_SOURCE="${BINARY_SOURCE:-./netbridge-agent}"
CONFIG_SOURCE="${CONFIG_SOURCE:-./configs/agent.yaml}"
UNIT_SOURCE="${UNIT_SOURCE:-./scripts/netbridge-agent.service}"
SYSTEMD_UNIT_PATH="/etc/systemd/system/${SERVICE_NAME}.service"

if [[ ! -f "${BINARY_SOURCE}" ]]; then
  echo "agent binary not found: ${BINARY_SOURCE}"
  exit 1
fi

if [[ ! -f "${CONFIG_SOURCE}" ]]; then
  echo "agent config not found: ${CONFIG_SOURCE}"
  exit 1
fi

mkdir -p "${INSTALL_DIR}/configs" "${INSTALL_DIR}/data"
install -m 755 "${BINARY_SOURCE}" "${INSTALL_DIR}/netbridge-agent"
install -m 644 "${CONFIG_SOURCE}" "${INSTALL_DIR}/configs/agent.yaml"

if command -v systemctl >/dev/null 2>&1 && [[ -f "${UNIT_SOURCE}" ]]; then
  sed \
    -e "s#/opt/netbridge-agent#${INSTALL_DIR}#g" \
    -e "s#netbridge-agent#${SERVICE_NAME}#g" \
    "${UNIT_SOURCE}" > "${SYSTEMD_UNIT_PATH}"
  systemctl daemon-reload
  systemctl enable "${SERVICE_NAME}"
  systemctl restart "${SERVICE_NAME}"
  echo "installed and started ${SERVICE_NAME}"
else
  echo "installed to ${INSTALL_DIR}"
  echo "systemd not detected, run manually:"
  echo "  ${INSTALL_DIR}/netbridge-agent -config ${INSTALL_DIR}/configs/agent.yaml"
fi
