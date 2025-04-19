#!/bin/bash

# ✅ Set environment variables for local DynamoDB
export AWS_REGION=us-east-1
export DYNAMODB_TABLE_NAME=task_management
export IS_LOCAL=true

echo "✅ Local environment variables set!"

# ✅ Build the project
echo "🔄 Building project..."
mvn clean install && mvn package

# ✅ Run the packaged JAR
echo "🚀 Starting Task Management Application..."
java -jar target/task-management-lambda-1.0-SNAPSHOT.jar
