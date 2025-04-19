package org.piyush.converters;

import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.piyush.model.taskmanagement.SubTask;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;

public class ListSubTaskConverter implements AttributeConverter<List<SubTask>> {
  private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

  @Override
  public AttributeValue transformFrom(List<SubTask> subTasks) {
    try {
      if (subTasks == null) {
        return AttributeValue.builder().nul(true).build();
      }
      String json = MAPPER.writeValueAsString(subTasks);
      return AttributeValue.builder().s(json).build();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialize SubTasks", e);
    }
  }

  @Override
  public List<SubTask> transformTo(AttributeValue attributeValue) {
    try {
      if (Boolean.TRUE.equals(attributeValue.nul())) {
        return Collections.emptyList();
      }
      return MAPPER.readValue(
          attributeValue.s(),
          MAPPER.getTypeFactory().constructCollectionType(List.class, SubTask.class)
      );
    } catch (Exception e) {
      throw new IllegalStateException("Failed to deserialize SubTasks", e);
    }
  }

  @Override
  public EnhancedType<List<SubTask>> type() {
    return EnhancedType.listOf(SubTask.class);
  }

  @Override
  public AttributeValueType attributeValueType() {
    return AttributeValueType.S;
  }
}
