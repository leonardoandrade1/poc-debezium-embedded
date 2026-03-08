-- Product microservice database initialization
USE product;

CREATE TABLE product (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL
);

-- Debezium control database (separate from business data)
CREATE DATABASE IF NOT EXISTS debezium;
USE debezium;

-- Debezium offset storage table
CREATE TABLE IF NOT EXISTS debezium_offset_storage (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    offset_key TEXT,
    offset_val TEXT,
    record_insert_ts TIMESTAMP NOT NULL,
    record_insert_seq INTEGER NOT NULL
);

-- Debezium schema history table
CREATE TABLE IF NOT EXISTS debezium_schema_history (
    id VARCHAR(255) NOT NULL,
    history_data TEXT,
    history_data_seq INTEGER NOT NULL,
    record_insert_ts TIMESTAMP NOT NULL,
    record_insert_seq INTEGER NOT NULL,
    PRIMARY KEY (id, history_data_seq)
);

