#!/usr/bin/env bash
# TICKET-ADV153 — Compose smoke-test script
# Goal: bring up the stack, wait for health, curl endpoints, tear down.

set -e

echo "==> Building backend..."
(cd backend && ./mvnw -q clean package -DskipTests)

echo "==> Starting docker-compose stack in detached mode..."
docker compose up -d --build

echo "==> Waiting for backend to become healthy (timeout 120s)..."
TIMEOUT=120
ELAPSED=0
while [ $ELAPSED -lt $TIMEOUT ]; do
  STATUS=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{end}}' reconx-backend 2>/dev/null || true)
  if [ "$STATUS" = "healthy" ]; then
    echo "Backend is healthy!"
    break
  fi
  echo "Still waiting... ($ELAPSED/$TIMEOUT seconds) - Status: $STATUS"
  sleep 5
  ELAPSED=$((ELAPSED+5))
done

if [ "$ELAPSED" -ge "$TIMEOUT" ]; then
  echo "ERROR: Backend did not become healthy in time."
  docker compose logs backend
  docker compose down -v
  exit 1
fi

echo "==> Running smoke tests..."

# 1. Test backend health
echo "Testing backend health endpoint..."
curl -s -f http://localhost:8080/api/actuator/health > /dev/null
echo "Backend health OK."

# 2. Test frontend availability
echo "Testing frontend root endpoint..."
curl -s -f -I http://localhost:5173 > /dev/null
echo "Frontend OK."

# 3. Test prometheus
echo "Testing Prometheus endpoint..."
curl -s -f -I http://localhost:9090 > /dev/null
echo "Prometheus OK."

echo "==> All smoke tests passed!"

echo "==> Tearing down the stack..."
docker compose down -v
echo "==> Clean teardown complete."
