#!/bin/bash
set -e

echo "Creating multiple databases for microservices architecture..."

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE vidasalud_appointments;
    CREATE DATABASE vidasalud_catalog;
    CREATE DATABASE vidasalud_audit;
    CREATE DATABASE vidasalud_report;
EOSQL

echo "Databases created successfully!"
