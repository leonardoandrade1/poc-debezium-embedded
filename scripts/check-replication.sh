#!/bin/bash

echo "--- Monolith ---"
docker exec -i mysql-monolith mysql -uroot -proot monolith -e "SELECT id, name, price FROM product;"

echo -e "\n--- Product Service ---"
docker exec -i mysql-product mysql -uroot -proot product -e "SELECT id, name, price FROM product;"

echo -e "\n--- Debezium Offsets ---"
docker exec -i mysql-product mysql -uroot -proot debezium -e "SELECT id, offset_key, offset_val FROM debezium_offset_storage;"
