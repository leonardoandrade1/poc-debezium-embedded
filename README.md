# Debezium Embedded MySQL CDC

This project demonstrates the **Strangler Fig Pattern** by replicating data from a Monolith database to a Microservice database using **Debezium Embedded**. It captures row-level changes (CDC) from MySQL and applies them directly to the destination database without requiring an external Kafka cluster.

## Architecture

*   **Source**: MySQL Monolith (Port 3307) - Tables: `product`, `orders`
*   **CDC Engine**: Debezium Embedded (Java 21 / Spring Boot 3.4)
*   **Destination**: MySQL Product Microservice (Port 3308) - Tables: `product`, `orders`
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

### 1. Insert a new product or order
Run the helper scripts to insert records into the Monolith:
```bash
./scripts/add-product.sh "Gaming Monitor" "4K 144Hz" 599.90
./scripts/add-order.sh 1 5 "buyer@example.com"
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

#### 4.7 Trigger Ad-Hoc Snapshot
**What are Ad-Hoc Incremental Snapshots?**
Normally, Debezium makes snapshot of all tables on the first run and then reads the change events by binlog. Sometines you need to resync specifics tables, for example, deleted data and need to restore it, or you added a new table and need to capture it.
In these cases, you can request this Ad-Hoc incremental snapshots by sending signals to Debezium, in this case, add a record in the `debezium_signal` table.
It won't stop the connector or stop reading binlogs, so you can continue using the applicaiton normally.
The table reading is done in chunks to avoid performance issues.
Important: The signal table must be created in the *source database*.

Refs:
- https://debezium.io/documentation/reference/stable/configuration/signalling.html#debezium-signaling-overview
- https://debezium.io/documentation/reference/stable/connectors/mysql.html#debezium-mysql-incremental-snapshots


```bash
./scripts/trigger-snapshot.sh
```

## Configuration Highlights

*   **Connector**: `MySqlConnector`
*   **Offset Storage**: `JdbcOffsetBackingStore` persist in destination MySQL.
*   **Data Handling**: `decimal.handling.mode=string` to ensure numeric precision in JSON.
*   **Lifecycle**: Managed by Spring `SmartLifecycle` to ensure clean startup and shutdown.
