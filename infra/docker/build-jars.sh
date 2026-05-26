#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

echo "Building common libraries (skip tests)..."
mvn -f "$ROOT_DIR/common/common-auth/pom.xml" -DskipTests install
mvn -f "$ROOT_DIR/common/common-exception/pom.xml" -DskipTests install
mvn -f "$ROOT_DIR/common/common-observability/pom.xml" -DskipTests install

echo "Building services (skip tests)..."
mvn -f "$ROOT_DIR/services/eureka-server/pom.xml" -DskipTests package
mvn -f "$ROOT_DIR/services/api-gateway/pom.xml" -DskipTests package
mvn -f "$ROOT_DIR/services/payment-orchestrator/pom.xml" -DskipTests package
mvn -f "$ROOT_DIR/services/fraud-service/pom.xml" -DskipTests package
mvn -f "$ROOT_DIR/services/ledger-service/pom.xml" -DskipTests package
mvn -f "$ROOT_DIR/services/notification-service/pom.xml" -DskipTests package

echo "Done. JARs should now exist under each service's target/ directory."
