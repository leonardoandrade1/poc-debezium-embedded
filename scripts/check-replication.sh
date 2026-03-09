#!/bin/bash

echo "--- Monolith: Products ---"
docker exec -i mysql-monolith mysql -uroot -proot monolith -e "SELECT id, name, price FROM product;"

echo -e "\n--- Monolith: Orders ---"
docker exec -i mysql-monolith mysql -uroot -proot monolith -e "SELECT id, product_id, quantity, customer_email FROM orders;"

echo -e "\n--- Product Service: Products ---"
docker exec -i mysql-product mysql -uroot -proot product -e "SELECT id, name, price FROM product;"

echo -e "\n--- Product Service: Orders ---"
docker exec -i mysql-product mysql -uroot -proot product -e "SELECT id, product_id, quantity, customer_email FROM orders;"

echo -e "\n--- Debezium Offsets ---"
docker exec -i mysql-product mysql -uroot -proot debezium -e "SELECT id, offset_key, offset_val FROM debezium_offset_storage;"
