# Substrax Platform

Substrax is a **fintech-grade, event-driven payment orchestration platform** designed to demonstrate how real-world payment systems are built internally at scale.

This project prioritizes **correctness, resilience, observability, and auditability** over CRUD simplicity. Every architectural choice mirrors patterns used in production financial systems.

---

## Table of Contents

* [What This Project Is](#what-this-project-is)
* [Core Design Principles](#core-design-principles)
* [High-Level Architecture](#high-level-architecture)
* [Payment Flow](#payment-flow)
* [Ledger & Reconciliation](#ledger--reconciliation)
* [Services Overview](#services-overview)
* [Runtime (Docker)](#runtime-docker)
* [Observability](#observability)
* [Security](#security)
* [Metabase](#metabase)
* [Apache Airflow](#apache-airflow)
* [Known Gaps](#known-gaps)
* [Roadmap](#roadmap)
* [Getting Started](#Prerequisites)

---

## What This Project Is

Substrax **is**:

* A distributed, event-driven backend platform
* A Saga-based payment orchestration system
* An immutable financial ledger implementation
* An observability-first microservices system

Substrax **is not**:

* A monolithic CRUD application
* A synchronous, request-blocking payment flow
* A UI-focused system
* A toy Kafka demo

---

## Core Design Principles

### 1. Event-Driven First

* Kafka is the backbone for all state transitions
* Services never block on each other
* HTTP is used only at system boundaries

### 2. Saga-Based Orchestration

* Distributed transactions are handled via Saga pattern
* Saga state is explicit and durable
* Duplicate and late events are safely ignored

### 3. Immutable Ledger

* Ledger entries are append-only
* No updates or deletes
* Full audit and replay capability

### 4. Asynchronous Reconciliation

* Payment flow is fast and non-blocking
* Accounting runs asynchronously via Airflow

### 5. Observability by Design

* Distributed tracing across HTTP and Kafka
* Logs correlated using traceId/spanId

### 6. Strong Event Contracts

* Kafka events encoded using Avro
* Schema Registry enforces compatibility

---

## High-Level Architecture

```
Client
  |
  v
API Gateway
  |
  v
Payment Orchestrator (Saga)
  |
  +--> Kafka (payment-events)
        |
        +--> Fraud Service
        |
        +--> Notification Service
        |
        +--> Ledger Service
                   |
                   +--> PostgreSQL (Immutable Ledger)
                   |
                   +--> S3 (Ledger Batches)
                            |
                            v
                         AWS Lambda
                            |
                            v
                         Apache Airflow
                            |
                            v
                   Reconciliation Tables
```

---

## Payment Flow

1. Client initiates payment via API Gateway
2. Payment Orchestrator creates transaction and saga
3. `PAYMENT_INITIATED` event published
4. Fraud Service evaluates and emits decision
5. Saga progresses based on fraud result
6. Ledger events emitted on success
7. Notifications recorded
8. Ledger batches exported and reconciled

---

## Ledger & Reconciliation

### Ledger

* PostgreSQL append-only table
* Represents immutable financial facts

### Reconciliation

* Ledger batches written to S3
* S3 event triggers AWS Lambda
* Lambda triggers Airflow DAG
* DAG validates debit/credit consistency

---

## Services Overview

| Service              | Description                | Port |
| -------------------- | -------------------------- | ---- |
| API Gateway          | Entry point, routing, auth | 8080 |
| Payment Orchestrator | Saga coordinator           | 8082 |
| Fraud Service        | Fraud evaluation           | 8083 |
| Ledger Service       | Immutable ledger           | 8084 |
| Notification Service | Notification records       | 8086 |
| Eureka Server        | Service discovery          | 8761 |

---

## Runtime (Docker)

All services run locally using Docker.

### Infrastructure

| Component         | Port |
| ----------------- | ---- |
| Kafka             | 9092 |
| Schema Registry   | 8085 |
| Zipkin            | 9411 |
| Kafka UI          | 8087 |
| Redis             | 6379 |
| Metabase          | 3000 |
| Airflow Webserver | 8088 |
| Keycloak          | 8081 |

---

## Observability

* Micrometer + Brave tracing
* Kafka producer & consumer spans enabled
* Zipkin for distributed trace visualization
* Structured logs with traceId/spanId

---

## Security

* OAuth2 Resource Server
* Keycloak issues JWT tokens
* API Gateway enforces authentication

---

## Metabase

Metabase is used for:

* Ledger validation
* Saga state inspection
* Data sanity checks

No business logic exists in the UI layer.

---

## Apache Airflow

* Runs in a separate Docker stack
* Event-driven DAGs (no cron)
* Triggered via AWS Lambda
* Used for ledger reconciliation

---

## Known Gaps

* Email/SMS delivery not implemented
* External payment providers not integrated
* Cloud deployment pending
* CI/CD pipelines pending

---

## Roadmap

* Notification delivery adapters
* Payment provider abstraction
* Cloud deployment (ECS/EKS)
* CI/CD pipelines
* Advanced saga compensation

---

## Repository

[https://github.com/nithinraaj27/substrax](https://github.com/nithinraaj27/substrax)

---

Substrax demonstrates **production-grade backend thinking**:

* Explicit state
* Failure-aware design
* Immutable data
* Observability-first engineering

This is how real payment systems are built.

---

# 🚀 Getting Started (Local Setup)

This project runs using **Docker Compose** and is split into **two execution layers**:

1. **Core Platform Stack** (Kafka, services, databases, observability)
2. **Airflow Stack** (Ledger reconciliation & batch orchestration)

⚠️ These stacks **must be started in the correct order**.

---

## Prerequisites

Ensure the following are installed:

- Docker ≥ 24.x
- Docker Compose v2
- Minimum **8 GB RAM** allocated to Docker

---

## 📦 1. Start Core Platform Services

From the **root of the repository**:

```bash
cd substrax-platform
```

- Start all core services:

```
docker compose up -d --build
```

## This starts:

- API Gateway
- Payment Orchestrator
- Fraud Service
- Ledger Service
- Notification Service
- Kafka
- Schema Registry
- Eureka Server
- Redis
- PostgreSQL databases
- Zipkin
- Metabase

Verify All the containers are running

```
docker ps -a
```
Please ensure all are up and running

## Startup Airflow

```
cd ../airflow/

docker compose -f docker-compose-airflow.yml down

docker compose -f docker-compose-airflow.yml up -d airflow-postgres

docker ps | grep airflow-postgres
```

🧠 Initialize Airflow Metadata DB (ONE-TIME STEP)

⚠️ This step is mandatory and must be executed before starting Airflow services.

```
docker compose -f docker-compose-airflow.yml run --rm airflow-webserver airflow db init
```

✅ Creates Airflow metadata tables
❌ Skipping this step will cause Airflow to fail


▶️ Start Airflow Webserver & Scheduler

```
docker compose -f docker-compose-airflow.yml up -d airflow-webserver airflow-scheduler

docker compose -f docker-compose-airflow.yml exec airflow-webserver airflow users create \
  --username admin \
  --password admin \
  --firstname Admin \
  --lastname User \
  --role Admin \
  --email admin@substrax.com
```

🌐 Access Airflow UI

Open in browser: http://localhost:8088

Login credentials:
- Username: admin
- Password: admin

## 🧠 Why This Order Matters

* Airflow does NOT auto-create its database schema.

## 🔍 Service URLs & Ports

* Eureka Dashboard	http://localhost:8761
* Kafka UI	http://localhost:8087
* Zipkin	http://localhost:9411
* Metabase	http://localhost:3000
* Airflow	http://localhost:8088
* Schema Registry: http://localhost:8087

  ✅ Final State

Once completed:

- Kafka-driven event flow is active
- Ledger entries are persisted
- Airflow performs reconciliation from S3
- Zipkin shows distributed traces
- Metabase enables data validation


