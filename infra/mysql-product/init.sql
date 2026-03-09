-- Product microservice database initialization
USE product;

CREATE TABLE IF NOT EXISTS product (
    id INT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    id INT PRIMARY KEY,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    order_date TIMESTAMP
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

