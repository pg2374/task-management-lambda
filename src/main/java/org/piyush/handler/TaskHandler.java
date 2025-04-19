package org.piyush.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.piyush.config.JacksonConfig;
import org.piyush.exception.TaskNotFoundException;
import org.piyush.exception.TaskRepositoryException;
import org.piyush.model.taskmanagement.TaskCreate;
import org.piyush.model.taskmanagement.TaskRead;
import org.piyush.model.taskmanagement.TaskUpdate;
import org.piyush.service.TaskService;
import org.piyush.utils.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class TaskHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private static final Logger logger = LoggerFactory.getLogger(TaskHandler.class);

  public static final String TASK_ID = "taskId";
  private static final String IS_REQUIRED = "Task_ID and Deadline is required";

  private final TaskService taskService;
  private final ObjectMapper objectMapper;

  // ✅ Add this default constructor for AWS Lambda
  public TaskHandler() {
    this.taskService = new TaskService(); // Ensure TaskService has a default constructor
    this.objectMapper = JacksonConfig.getObjectMapper();
  }

  public TaskHandler(TaskService taskService) {
    this.taskService = taskService;
    this.objectMapper = JacksonConfig.getObjectMapper();
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
    String requestId = context.getAwsRequestId();

    // ✅ Ensure input is not null
    if (input == null) {
      logger.error("Received null request. RequestId: {}", requestId);
      return buildResponse(400, "Bad Request: No input provided", null);
    }

    logger.info("Processing {} request. RequestId: {}", input.getHttpMethod(), requestId);
    try {

      String httpMethod = input.getHttpMethod();
      if (httpMethod == null) {
        logger.error("HTTP Method is null. RequestId: {}", requestId);
        return buildResponse(400, "Bad Request: Missing HTTP Method", null);
      }

      return switch (input.getHttpMethod()) {
        case "POST" -> handleCreate(input, requestId);
        case "GET" -> handleGet(input, requestId);
        case "PUT" -> handleUpdate(input, requestId);
        case "DELETE" -> handleDelete(input, requestId);
        default -> buildResponse(405, "Method not allowed", null);
      };
    } catch (Exception e) {
      logger.error("Error processing request. RequestId: {}", requestId, e);
      return handleError(e);
    }
  }

  private APIGatewayProxyResponseEvent handleCreate(APIGatewayProxyRequestEvent input, String requestId) {
    try {
      TaskCreate taskCreate = objectMapper.readValue(input.getBody(), TaskCreate.class);
      ValidationUtil.validate(taskCreate);
      TaskRead result = taskService.createTask(taskCreate);
      logger.info("Task created successfully with ID: {}. RequestId: {}", result.getId(), requestId);
      return buildResponse(201, null, result);
    } catch (Exception e) {
      logger.error("Create task failed. RequestId: {}", requestId, e);
      return handleError(e);
    }
  }

  private APIGatewayProxyResponseEvent handleGet(APIGatewayProxyRequestEvent input, String requestId) {
    try {
      String taskId = extractTaskId(input);
      String deadlineStr = extractDeadline(input);
      if (taskId == null || deadlineStr == null) {
        return buildResponse(400, IS_REQUIRED, null);
      }

      Instant deadline = Instant.parse(deadlineStr);
      TaskRead taskRead = taskService.getTask(taskId, deadline);
      logger.info("Task retrieved successfully with ID: {}. RequestId: {}", taskId, requestId);
      return buildResponse(200, null, taskRead);
    } catch (TaskNotFoundException e) {
      return buildResponse(404, e.getMessage(), null); // ✅ Ensures correct error handling
    } catch (Exception e) {
      return handleError(e);
    }
  }

  private APIGatewayProxyResponseEvent handleUpdate(APIGatewayProxyRequestEvent input, String requestId) {
    try {
      String taskId = extractTaskId(input);
      if (taskId == null) {
        return buildResponse(400, IS_REQUIRED, null);
      }

      TaskUpdate taskUpdate = objectMapper.readValue(input.getBody(), TaskUpdate.class);
      ValidationUtil.validate(taskUpdate);

      if (!taskId.equals(taskUpdate.getId())) {
        return buildResponse(400, "ID in URL does not match ID in the payload", null);
      }

      TaskRead updatedTask = taskService.updateTask(taskUpdate);

      logger.info("Task updated successfully with ID: {}. RequestId: {}", updatedTask.getId(), requestId);
      return buildResponse(200, null, updatedTask);
    } catch (Exception e) {
      logger.error("Error updating task. RequestId: {}", requestId, e);
      return handleError(e);
    }
  }

  private APIGatewayProxyResponseEvent handleDelete(APIGatewayProxyRequestEvent input, String requestId) {
    try {
      String taskId = extractTaskId(input);
      if (taskId == null) {
        return buildResponse(400, IS_REQUIRED, null);
      }

      taskService.deleteTask(taskId);

      logger.info("Task deleted successfully with ID: {}. RequestId: {}", taskId, requestId);
      return buildResponse(204, null, null);
    } catch (Exception e) {
      logger.error("Error deleting task. RequestId: {}", requestId, e);
      return handleError(e);
    }
  }

  private String extractTaskId(APIGatewayProxyRequestEvent input) {
    return input.getPathParameters() != null ? input.getPathParameters().get(TASK_ID) : null;
  }

  private String extractDeadline(APIGatewayProxyRequestEvent input) {
    return input.getPathParameters() != null ? input.getPathParameters().get("deadline") : null;
  }

  private APIGatewayProxyResponseEvent handleError(Exception e) {
    if (e instanceof TaskNotFoundException) {
      return buildResponse(404, e.getMessage(), null); // ✅ Ensure missing task returns 404
    } else if (e instanceof TaskRepositoryException) {
      return buildResponse(400, e.getMessage(), null); // ✅ Ensure DB error returns 400
    } else if (e instanceof IllegalArgumentException) {
      return buildResponse(400, e.getMessage(), null); // ✅ Ensures missing parameters return 400
    } else {
      return buildResponse(500, "Internal server error", null); // ✅ Ensure unexpected errors return 500
    }
  }

  private APIGatewayProxyResponseEvent buildResponse(int statusCode, String message, Object data) {
    var response = new APIGatewayProxyResponseEvent().withStatusCode(statusCode).withHeaders(getCorsHeaders());

    Map<String, Object> body = new HashMap<>();
    if (message != null) body.put("message", message);
    if (data != null) body.put("data", data);

    try {
      response.setBody(objectMapper.writeValueAsString(body));
    } catch (Exception e) {
      logger.error("Error serializing response body", e);
      response.setStatusCode(500);
      response.setBody("{\"message\":\"Internal server error\"}");
    }
    return response;
  }

  private Map<String, String> getCorsHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Access-Control-Allow-Origin", "*");
    headers.put("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token");
    headers.put("Access-Control-Allow-Methods", "POST,GET,PUT,DELETE");
    headers.put("Content-Type", "application/json");
    return headers;
  }
}
