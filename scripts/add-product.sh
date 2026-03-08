#!/bin/bash

# Default values
NAME=${1:-"Product test $(date +%s)"}
DESCRIPTION=${2:-"Description generated via test script"}
PRICE=${3:-"99.90"}

echo "--- Inserting product in Monolith ---"
echo "Name: $NAME"
echo "Descrição: $DESCRIPTION"
echo "Preço: $PRICE"

docker exec -i mysql-monolith mysql -uroot -proot monolith -e \
    "INSERT INTO product (name, description, price) VALUES ('$NAME', '$DESCRIPTION', $PRICE);"

if [ $? -eq 0 ]; then
    echo "✅ Product inserted successfully!"
    echo "--- Wait a few seconds for CDC replication ---"
else
    echo "❌ Error inserting product."
fi
