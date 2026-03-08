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
    ('Notebook Dell Inspiron', 'Notebook Dell Inspiron 15, 16GB RAM, 512GB SSD', 4599.90),
    ('Mouse Logitech MX Master', 'Mouse sem fio ergonômico com sensor de alta precisão', 499.90),
    ('Teclado Mecânico Keychron K2', 'Teclado mecânico 75%, switches Gateron Brown, RGB', 699.90),
    ('Monitor LG UltraWide 34"', 'Monitor ultrawide 34 polegadas, resolução WQHD', 3299.90),
    ('Headset HyperX Cloud II', 'Headset gamer com som surround 7.1 virtual', 399.90);
