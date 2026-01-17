from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.utils.dates import days_ago
from airflow.exceptions import AirflowFailException

import json
import boto3
import psycopg2
import hashlib
import re
from decimal import Decimal
from datetime import datetime

# -------------------------
# CONFIG
# -------------------------
BUCKET_NAME = "substrax-ledger-bucket"

DB_CONFIG = {
    "host": "ledger-db",
    "port": 5432,
    "dbname": "ledgerdb",
    "user": "ledgeruser",
    "password": "ledgerpass"
}

# -------------------------
# TASK 0: VALIDATE MANIFEST
# -------------------------
def validate_manifest(**context):
    ti = context["ti"]
    conf = context["dag_run"].conf

    bucket = conf["bucket"]
    manifest_key = conf["key"]

    if not manifest_key.endswith(".manifest.json"):
        raise AirflowFailException("Expected manifest file trigger")

    # Derive ledger data file key
    ledger_key = manifest_key.replace(".manifest.json", "")

    s3 = boto3.client("s3")

    # ---- Read manifest ----
    manifest_obj = s3.get_object(Bucket=bucket, Key=manifest_key)
    manifest = json.loads(manifest_obj["Body"].read())

    # ---- Read ledger file ----
    ledger_obj = s3.get_object(Bucket=bucket, Key=ledger_key)
    ledger_bytes = ledger_obj["Body"].read()

    # ---- Validate hash ----
    actual_hash = hashlib.sha256(ledger_bytes).hexdigest()
    expected_hash = manifest["sha256"]

    if actual_hash != expected_hash:
        raise AirflowFailException(
            f"Ledger hash mismatch. Expected={expected_hash}, Actual={actual_hash}"
        )

    # ---- Validate record count ----
    lines = ledger_bytes.decode("utf-8").splitlines()
    actual_count = len(lines)
    expected_count = manifest["recordCount"]

    if actual_count != expected_count:
        raise AirflowFailException(
            f"Record count mismatch. Expected={expected_count}, Actual={actual_count}"
        )

    # ---- Extract batch_id ----
    match = re.search(r"(LEDGER-BATCH-\d+)", ledger_key)
    if not match:
        raise AirflowFailException("Invalid ledger file name format")

    batch_id = match.group(1)

    # ---- Push validated values ----
    ti.xcom_push(key="bucket", value=bucket)
    ti.xcom_push(key="ledger_key", value=ledger_key)
    ti.xcom_push(key="batch_id", value=batch_id)
    ti.xcom_push(key="currency", value=manifest["currency"])

    print(f"Manifest validated successfully for batch {batch_id}")

# -------------------------
# TASK 1: READ LEDGER FILE
# -------------------------
def read_ledger_batch_from_s3(**context):
    ti = context["ti"]

    bucket = ti.xcom_pull(key="bucket")
    ledger_key = ti.xcom_pull(key="ledger_key")

    s3 = boto3.client("s3")

    response = s3.get_object(Bucket=bucket, Key=ledger_key)
    lines = response["Body"].read().decode("utf-8").splitlines()

    if not lines:
        raise AirflowFailException("Ledger file is empty")

    records = [json.loads(line) for line in lines]

    ti.xcom_push(key="ledger_records", value=records)

# -------------------------
# TASK 2: RECONCILE + STORE
# -------------------------
def reconcile_and_store(**context):
    ti = context["ti"]

    records = ti.xcom_pull(key="ledger_records")
    batch_id = ti.xcom_pull(key="batch_id")
    currency = ti.xcom_pull(key="currency")

    total_debits = Decimal("0")
    total_credits = Decimal("0")
    total_refunds = Decimal("0")

    for r in records:
        amount = Decimal(str(r["amount"]))
        event_type = r["event_type"]

        if event_type == "LEDGER_DEBIT":
            total_debits += amount
        elif event_type == "LEDGER_CREDIT":
            total_credits += amount
        elif event_type == "LEDGER_REFUND":
            total_refunds += amount

    difference = total_debits - (total_credits + total_refunds)
    status = "MATCHED" if difference == 0 else "MISMATCHED"

    conn = psycopg2.connect(**DB_CONFIG)
    cur = conn.cursor()

    cur.execute("""
        INSERT INTO ledger_reconciliation (
            batch_id,
            currency,
            total_debits,
            total_credits,
            total_refunds,
            difference,
            record_count,
            reconciliation_status,
            reconciled_at
        )
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s)
        ON CONFLICT (batch_id) DO NOTHING
    """, (
        batch_id,
        currency,
        total_debits,
        total_credits,
        total_refunds,
        difference,
        len(records),
        status,
        datetime.utcnow()
    ))

    conn.commit()
    cur.close()
    conn.close()

    print(f"Reconciliation completed for batch {batch_id} with status {status}")

# -------------------------
# DAG DEFINITION
# -------------------------
with DAG(
    dag_id="ledger_reconciliation_dag",
    start_date=days_ago(1),
    schedule_interval=None,   # Event-driven only
    catchup=False,
    tags=["ledger", "reconciliation"],
) as dag:

    validate_manifest_task = PythonOperator(
        task_id="validate_manifest",
        python_callable=validate_manifest,
    )

    read_from_s3 = PythonOperator(
        task_id="read_ledger_batch_from_s3",
        python_callable=read_ledger_batch_from_s3,
    )

    reconcile = PythonOperator(
        task_id="reconcile_and_store",
        python_callable=reconcile_and_store,
    )

    validate_manifest_task >> read_from_s3 >> reconcile
