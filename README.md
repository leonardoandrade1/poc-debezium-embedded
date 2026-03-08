# Debezium Embedded MySQL CDC

This project demonstrates the **Strangler Fig Pattern** by replicating data from a Monolith database to a Microservice database using **Debezium Embedded**. It captures row-level changes (CDC) from MySQL and applies them directly to the destination database without requiring an external Kafka cluster.

## Architecture

*   **Source**: MySQL Monolith (Port 3307) - Table `product`
*   **CDC Engine**: Debezium Embedded (Java 21 / Spring Boot 3.4)
*   **Destination**: MySQL Product Microservice (Port 3308) - Table `product`
*   **Control**: Dedicated `debezium` database for offsets and schema history.

## Prerequisites

*   Docker and Docker Compose
*   Java 21 (optional, if running locally)

## Getting Started

1.  **Start the environment**:
    ```bash
    docker compose down -v && docker compose up -d --build
    ```
    *This will start both MySQL instances and the Spring Boot application.*

2.  **Verify Initial Snapshot**:
    The application automatically performs an initial snapshot of the 5 products existing in the Monolith. Check the destination:
    ```bash
    ./scripts/check-replication.sh
    ```

## Testing CDC Replication

### 1. Insert a new product
Run the helper script to insert a record into the Monolith:
```bash
./scripts/add-product.sh "Gaming Monitor" "4K 144Hz" 599.90
```

### 2. Verify the change
Check the Microservice database to see the replicated record:
```bash
./scripts/check-replication.sh
```

### 3. REST API
You can also interact with the Microservice data via its REST API:
*   `GET http://localhost:8080/api/products`
*   Example calls are available in `http/products.http`.

### 4. More scripts
#### 4.1 Verify initial snapshot
```bash
docker exec -it mysql-product mysql -uroot -proot -e "SELECT * FROM product.product;"
```

#### 4.2 Insert data
```bash
docker exec -it mysql-monolith mysql -uroot -proot -e \
  "INSERT INTO monolith.product (name, description, price) VALUES ('New Product', 'CDC test product description', 29.90);"
```

#### 4.3 Check replication
```bash
docker exec -it mysql-product mysql -uroot -proot -e "SELECT * FROM product.product;"
```

#### 4.4 Update data
```bash
docker exec -it mysql-monolith mysql -uroot -proot -e \
  "UPDATE monolith.product SET price = 99.90 WHERE id = 1;"
```

#### 4.5 Delete data
```bash
docker exec -it mysql-monolith mysql -uroot -proot -e \
  "DELETE FROM monolith.product WHERE id = 2;"
```

#### 4.6 Check offsets
```bash
docker exec -it mysql-product mysql -uroot -proot -e "SELECT * FROM product.debezium_offset_storage;"
```

## Configuration Highlights

*   **Connector**: `MySqlConnector`
*   **Offset Storage**: `JdbcOffsetBackingStore` persist in destination MySQL.
*   **Data Handling**: `decimal.handling.mode=string` to ensure numeric precision in JSON.
*   **Lifecycle**: Managed by Spring `SmartLifecycle` to ensure clean startup and shutdown.
