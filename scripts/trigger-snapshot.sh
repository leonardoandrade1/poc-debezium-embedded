#!/bin/bash

echo "Triggering ad-hoc snapshot for monolith.orders..."

docker exec -it mysql-monolith mysql -uroot -proot monolith -e \
  "INSERT INTO debezium_signal (id, type, data) VALUES (UUID(), 'execute-snapshot', '{\"data-collections\": [\"monolith.orders\"], \"type\": \"incremental\"}');"

echo "Snapshot signal sent successfully! Check the application logs to see the snapshot progress."
