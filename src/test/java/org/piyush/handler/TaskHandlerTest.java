package org.piyush.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.piyush.config.JacksonConfig;
import org.piyush.constant.Priority;
import org.piyush.exception.TaskNotFoundException;
import org.piyush.exception.TaskRepositoryException;
import org.piyush.model.taskmanagement.TaskCreate;
import org.piyush.model.taskmanagement.TaskRead;
import org.piyush.model.taskmanagement.TaskUpdate;
import org.piyush.service.TaskService;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskHandlerTest {

  private static final String TEST_ID = "test-id";
  private static final String TEST_TITLE = "Test Task";
  private static final Instant TEST_DEADLINE = Instant.parse("2025-02-18T10:00:00Z");

  @Mock
  private TaskService taskService;

  @InjectMocks
  private TaskHandler taskHandler;

  @BeforeEach
  void setup() {
    taskHandler = new TaskHandler(taskService);
  }

  // SUCCESS SCENARIOS

  @Test
  void handleRequest_CreateSuccess() throws JsonProcessingException {
    TaskCreate taskCreate = (TaskCreate) new TaskCreate()
        .setTitle(TEST_TITLE)
        .setDeadline(TEST_DEADLINE)
        .setPriority(Priority.LOW);

    TaskRead expectedResponse = (TaskRead) new TaskRead()
        .setId(TEST_ID)
        .setTitle(TEST_TITLE);

    APIGatewayProxyRequestEvent request = createApiRequest("POST", taskCreate);
    when(taskService.createTask(any(TaskCreate.class))).thenReturn(expectedResponse);

    APIGatewayProxyResponseEvent response = taskHandler.handleRequest(request, createMockContext());

    assertThat(response.getStatusCode()).isEqualTo(201);
    assertThat(response.getBody()).contains("\"id\":\"test-id\"", "\"title\":\"Test Task\"");
    verify(taskService).createTask(any(TaskCreate.class));
  }

//  @Test
//  void handleRequest_GetSuccess() {
//    TaskRead expectedResponse = (TaskRead) new TaskRead()
//        .setId(TEST_ID)
//        .setTitle(TEST_TITLE)
//        .setDeadline(TEST_DEADLINE);
//
//    APIGatewayProxyRequestEvent request = createGetRequest();
//    when(taskService.getTask(TEST_ID, TEST_DEADLINE)).thenReturn(expectedResponse);
//
//    APIGatewayProxyResponseEvent response = taskHandler.handleRequest(request, createMockContext());
//
//    assertThat(response.getStatusCode()).isEqualTo(200);
//    assertThat(response.getBody()).contains("\"id\":\"test-id\"", "\"title\":\"Test Task\"", "\"deadline\":\"2025-02-12T00:00:00Z\"");
//    verify(taskService).getTask(TEST_ID);
//  }

  @Test
  void handleRequest_UpdateSuccess() throws JsonProcessingException {
    TaskUpdate taskUpdate = (TaskUpdate) new TaskUpdate()
        .setId(TEST_ID)
        .setPriority(Priority.HIGH)
        .setTitle("Updated Task");

    TaskRead expectedResponse = (TaskRead) new TaskRead()
        .setId(TEST_ID)
        .setPriority(Priority.HIGH)
        .setTitle("Updated Task");

    APIGatewayProxyRequestEvent request = createApiRequest("PUT", taskUpdate)
        .withPathParameters(Map.of("taskId", TEST_ID));

    when(taskService.updateTask(any(TaskUpdate.class))).thenReturn(expectedResponse);

    APIGatewayProxyResponseEvent response = taskHandler.handleRequest(request, createMockContext());

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains("\"id\":\"test-id\"", "\"title\":\"Updated Task\"");
    verify(taskService).updateTask(any(TaskUpdate.class));
  }

  @Test
  void handleRequest_DeleteSuccess() {
    APIGatewayProxyRequestEvent request = createDeleteRequest();

    APIGatewayProxyResponseEvent response = taskHandler.handleRequest(request, createMockContext());

    assertThat(response.getStatusCode()).isEqualTo(204);
    verify(taskService).deleteTask(TEST_ID);
  }

  // VALIDATION SCENARIOS

  @ParameterizedTest
  @MethodSource("provideValidationTestCases")
  void handleRequest_ValidationScenarios(String httpMethod, int expectedStatus, String expectedMessage) {
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withHttpMethod(httpMethod);

    APIGatewayProxyResponseEvent response = taskHandler.handleRequest(request, createMockContext());

    assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
    assertThat(response.getBody()).contains(expectedMessage);
  }

  @Test
  void handleRequest_UpdateWithoutTaskId() throws JsonProcessingException {
    TaskUpdate taskUpdate = (TaskUpdate) new TaskUpdate()
        .setId(TEST_ID)
        .setTitle("Updated Task");

    APIGatewayProxyRequestEvent request = createApiRequest("PUT", taskUpdate);

    APIGatewayProxyResponseEvent response = taskHandler.handleRequest(request, createMockContext());

    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBody()).contains("Task_ID and Deadline is required");
  }

  @Test
  void handleRequest_UpdateMismatchedIds() throws JsonProcessingException {
    TaskUpdate taskUpdate = (TaskUpdate) new TaskUpdate()
        .setId("different-id")
        .setPriority(Priority.MEDIUM)
        .setTitle("Updated Task");

    APIGatewayProxyRequestEvent request = createApiRequest("PUT", taskUpdate)
        .withPathParameters(Map.of("taskId", TEST_ID));

    APIGatewayProxyResponseEvent response = taskHandler.handleRequest(request, createMockContext());

    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBody()).contains("ID in URL does not match ID in the payload");
  }

  // ERROR HANDLING SCENARIOS

//  @ParameterizedTest
//  @MethodSource("provideErrorScenarios")
//  void handleRequest_ErrorHandling(String httpMethod, String taskId, Throwable exception,
//                                   int expectedStatus, String expectedMessage) {
//    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
//        .withHttpMethod(httpMethod)
//        .withPathParameters(Map.of("taskId", taskId));
//
//    doThrow(exception).when(taskService).getTask(taskId);
//
//    APIGatewayProxyResponseEvent response = taskHandler.handleRequest(request, createMockContext());
//
//    assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
//    assertThat(response.getBody()).contains(expectedMessage);
//  }

  @Test
  void handleRequest_InvalidJsonCreate() {
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withHttpMethod("POST")
        .withBody("invalid json");

    APIGatewayProxyResponseEvent response = taskHandler.handleRequest(request, createMockContext());

    assertThat(response.getStatusCode()).isEqualTo(500);
    assertThat(response.getBody()).contains("Internal server error");
  }

  @Test
  void handleRequest_InvalidJsonUpdate() {
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withHttpMethod("PUT")
        .withPathParameters(Map.of("taskId", TEST_ID))
        .withBody("invalid json");

    APIGatewayProxyResponseEvent response = taskHandler.handleRequest(request, createMockContext());

    assertThat(response.getStatusCode()).isEqualTo(500);
    assertThat(response.getBody()).contains("Internal server error");
  }

//  @Test
//  void handleRequest_SerializationError() throws Exception {
//    TaskRead taskRead = (TaskRead) new TaskRead()
//        .setId(TEST_ID)
//        .setTitle(TEST_TITLE)
//        .setPriority(Priority.MEDIUM);
//
//    APIGatewayProxyRequestEvent request = createGetRequest();
//    ObjectMapper mockMapper = configureMockObjectMapper();
//    when(taskService.getTask(TEST_ID, TEST_DEADLINE)).thenReturn(taskRead);
//
//    APIGatewayProxyResponseEvent response = taskHandler.handleRequest(request, createMockContext());
//
//    assertThat(response.getStatusCode()).isEqualTo(500);
//    assertThat(response.getBody()).contains("Internal server error");
//    verify(taskService).getTask(TEST_ID);
//    verify(mockMapper).writeValueAsString(any());
//  }

  @Test
  void handleRequest_DeleteError() {
    APIGatewayProxyRequestEvent request = createDeleteRequest();
    doThrow(new RuntimeException("Unexpected deletion error"))
        .when(taskService).deleteTask(TEST_ID);

    APIGatewayProxyResponseEvent response = taskHandler.handleRequest(request, createMockContext());

    assertThat(response.getStatusCode()).isEqualTo(500);
    assertThat(response.getBody()).contains("Internal server error");
    verify(taskService).deleteTask(TEST_ID);
  }

  @Test
  void handleRequest_MethodNotAllowed() {
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withHttpMethod("UNKNOWN");

    APIGatewayProxyResponseEvent response = taskHandler.handleRequest(request, createMockContext());

    assertThat(response.getStatusCode()).isEqualTo(405);
    assertThat(response.getBody()).contains("Method not allowed");
  }

  // UTILITY METHODS

  private static Stream<Arguments> provideValidationTestCases() {
    return Stream.of(
        Arguments.of("PATCH", 405, "Method not allowed"),
        Arguments.of("GET", 400, "Task_ID and Deadline is required"),
        Arguments.of("DELETE", 400, "Task_ID and Deadline is required")
    );
  }

  private static Stream<Arguments> provideErrorScenarios() {
    return Stream.of(
        // ✅ Missing task should return 404
        Arguments.of("GET", "non-existent-id",
            new TaskNotFoundException("Task not found"), 404, "Task not found"),

        // ✅ Database error should return 400
        Arguments.of("GET", "invalid-id",
            new TaskRepositoryException("Database error"), 400, "Database error"),

        // ✅ Missing Task_ID and Deadline should return 400
        Arguments.of("GET", null,
            new IllegalArgumentException("Task_ID and Deadline is required"), 400, "Task_ID and Deadline is required"),

        // ✅ Unexpected errors should return 500
        Arguments.of("GET", "test-id",
            new RuntimeException("Unexpected error"), 500, "Internal server error")
    );
  }

  private static Context createMockContext() {
    Context mockContext = mock(Context.class);
    when(mockContext.getAwsRequestId()).thenReturn("test-request-id");
    return mockContext;
  }

  private static APIGatewayProxyRequestEvent createApiRequest(String httpMethod, Object payload)
      throws JsonProcessingException {
    ObjectMapper objectMapper = JacksonConfig.getObjectMapper();
    return new APIGatewayProxyRequestEvent()
        .withHttpMethod(httpMethod)
        .withBody(objectMapper.writeValueAsString(payload));
  }

  private static APIGatewayProxyRequestEvent createGetRequest() {
    return new APIGatewayProxyRequestEvent()
        .withHttpMethod("GET")
        .withPathParameters(Map.of("taskId", TaskHandlerTest.TEST_ID));
  }

  private static APIGatewayProxyRequestEvent createDeleteRequest() {
    return new APIGatewayProxyRequestEvent()
        .withHttpMethod("DELETE")
        .withPathParameters(Map.of("taskId", TaskHandlerTest.TEST_ID));
  }

  private ObjectMapper configureMockObjectMapper() throws Exception {
    ObjectMapper mockMapper = spy(JacksonConfig.getObjectMapper());
    doThrow(new JsonProcessingException("Serialization error") {
    })
        .when(mockMapper).writeValueAsString(any());

    Field mapperField = TaskHandler.class.getDeclaredField("objectMapper");
    mapperField.setAccessible(true);
    mapperField.set(taskHandler, mockMapper);

    return mockMapper;
  }
}