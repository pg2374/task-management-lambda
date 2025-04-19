package org.piyush.repositories.base;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;


public abstract class DynamoDBBaseRepositoryTest {
  protected static final String TABLE_NAME = "task_management";
  private static DynamoDBProxyServer server;

  @BeforeAll
  public static void setupClass() throws Exception {
    System.setProperty("sqlite4java.library.path", "target/native-libs");
    System.setProperty("IS_LOCAL", "true");
    System.setProperty("DYNAMODB_TABLE_NAME", TABLE_NAME);
    String port = "8000";
    server = ServerRunner.createServerFromCommandLineArgs(
        new String[]{"-inMemory", "-port", port}
    );
    server.start();
  }

  @AfterAll
  public static void teardownClass() throws Exception {
    if (server != null) {
      server.stop();
    }
  }
}
