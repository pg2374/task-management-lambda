package org.piyush.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DynamoDbConfig {
  // ✅ Detects whether running in a test environment
  private static final boolean IS_TEST = System.getProperty("IS_TEST") != null;

  // ✅ Reads environment variables
  private static final String AWS_REGION = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
  private static final String TABLE_NAME = System.getenv().getOrDefault("DYNAMODB_TABLE_NAME", "task_management");
  private static final boolean IS_LOCAL = Boolean.parseBoolean(System.getenv().getOrDefault("IS_LOCAL", "false"));

  private static final class EnhancedClientHolder {
    private static final DynamoDbEnhancedClient enhancedClient = createClient();

    private static DynamoDbEnhancedClient createClient() {
      return DynamoDbEnhancedClient.builder()
          .dynamoDbClient(createDynamoDbClient())
          .build();
    }

    private static DynamoDbClient createDynamoDbClient() {
      if (IS_LOCAL || IS_TEST) {
        System.out.println("🔹 Using Local DynamoDB for Testing");
        return DynamoDbClient.builder()
            .endpointOverride(URI.create("http://localhost:8000"))
            .region(Region.of(AWS_REGION))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("DUMMYIDEXAMPLE", "DUMMYEXAMPLEKEY")))
            .build();
      } else {
        System.out.println("☁️ Using AWS DynamoDB");
        return DynamoDbClient.builder()
            .region(Region.of(AWS_REGION))
            .credentialsProvider(DefaultCredentialsProvider.create()) // Uses IAM role in AWS
            .build();
      }
    }
  }

  public static DynamoDbEnhancedClient dynamoDbEnhancedClient() {
    return EnhancedClientHolder.enhancedClient;
  }
}
