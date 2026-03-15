-- Monolith database initialization
USE monolith;

CREATE TABLE product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL
);

-- Sample data
INSERT INTO product (name, description, price) VALUES
    ('Smartphone', 'Latest model smartphone', 999.99),
    ('Laptop', 'Powerfull gaming laptop', 1500.00),
    ('Headphones', 'Noise-cancelling headphones', 299.99),
    ('Watch', 'Smartwatch with heart rate monitor', 199.99),
    ('Tablet', '10-inch tablet with stylus', 499.99);

-- New Orders table
CREATE TABLE IF NOT EXISTS orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO orders (product_id, quantity, customer_email) VALUES
    (1, 1, 'customer1@example.com'),
    (2, 2, 'customer2@example.com');

-- Debezium signal table to trigger debezium actions (https://debezium.io/documentation/reference/stable/configuration/signalling.html#debezium-signaling-overview)
CREATE TABLE IF NOT EXISTS debezium_signal (
    id VARCHAR(42) PRIMARY KEY,
    type VARCHAR(32) NOT NULL,
    data VARCHAR(2048) NULL
);

-- Example of a signal to trigger a snapshot
-- INSERT INTO debezium_signal (id, type, data) VALUES (UUID(), 'execute-snapshot', '{"data-collections": ["monolith.orders"], "type": "incremental"}');
