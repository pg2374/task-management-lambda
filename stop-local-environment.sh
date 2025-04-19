#!/bin/bash

echo "🛑 Stopping Task Management Application..."

# ✅ Unset environment variables
unset AWS_REGION
unset DYNAMODB_TABLE_NAME
unset IS_LOCAL

echo "✅ Environment variables unset."

# ✅ Stop running DynamoDB Local (if running with Docker)
if [[ "$(docker ps -q --filter ancestor=amazon/dynamodb-local)" ]]; then
    echo "🛑 Stopping DynamoDB Local..."
    docker stop $(docker ps -q --filter ancestor=amazon/dynamodb-local)
fi

echo "✅ Local environment stopped!"
