#!/bin/bash

# Default values
PRODUCT_ID=${1:-1}
QUANTITY=${2:-10}
EMAIL=${3:-"test@example.com"}

echo "--- Inserting order in Monolith ---"
echo "Product ID: $PRODUCT_ID"
echo "Quantity: $QUANTITY"
echo "Email: $EMAIL"

docker exec -i mysql-monolith mysql -uroot -proot monolith -e \
    "INSERT INTO orders (product_id, quantity, customer_email) VALUES ($PRODUCT_ID, $QUANTITY, '$EMAIL');"

if [ $? -eq 0 ]; then
    echo "✅ Order inserted successfully!"
    echo "--- Wait a few seconds for CDC replication ---"
else
    echo "❌ Error inserting order."
fi
