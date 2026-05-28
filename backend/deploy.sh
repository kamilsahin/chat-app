#!/bin/bash
set -e

DEPLOY_DIR=/home/activityzone/chat
JAR_NAME=chat.jar
SERVICE=chat.service

echo "[deploy] Starting deployment..."

mv /tmp/chat-app.jar.new "$DEPLOY_DIR/$JAR_NAME"
echo "[deploy] JAR updated."

sudo systemctl restart "$SERVICE"
echo "[deploy] Service restarted."

sleep 5
if systemctl is-active --quiet "$SERVICE"; then
  echo "[deploy] $SERVICE is running. Deployment successful."
else
  echo "[deploy] ERROR: $SERVICE failed to start!"
  sudo journalctl -u "$SERVICE" -n 30 --no-pager
  exit 1
fi
