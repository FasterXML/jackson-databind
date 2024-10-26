package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue4752Test extends DatabindTestUtil {

  private final ObjectMapper objectMapper = newJsonMapper();

  @JsonAutoDetect(
      fieldVisibility = JsonAutoDetect.Visibility.ANY,
      getterVisibility = JsonAutoDetect.Visibility.NONE
  )
  static class FirstObject {

    @JsonAnySetter
    @JsonAnyGetter
    private Map<String, Object> data = new LinkedHashMap<>();

    public String getTransactionId() {
      return (String) data.get("transactionId");
    }
  }

  @JsonAutoDetect(
      fieldVisibility = JsonAutoDetect.Visibility.ANY,
      getterVisibility = JsonAutoDetect.Visibility.NONE
  )
  static class SecondObject {

    private final String transactionId;
    @JsonAnySetter
    @JsonAnyGetter
    private final Map<String, Object>  data;


    @JsonCreator
    public SecondObject(@JsonProperty("transactionId") String transactionId) {
      this.transactionId = transactionId;
      this.data = new LinkedHashMap<>();
    }

    public String getTransactionId() {
      return this.transactionId;
    }

    public Map<String, Object> getData() {
      return this.data;
    }
  }

  @JsonAutoDetect(
      fieldVisibility = JsonAutoDetect.Visibility.ANY,
      getterVisibility = JsonAutoDetect.Visibility.NONE
  )
  static class ThirdObject {

    private final String transactionId;
    @JsonAnySetter
    @JsonAnyGetter
    private final Map<String, Object> data;


    @JsonCreator
    public ThirdObject(
        @JsonProperty("transactionId") String transactionId,
        @JsonProperty("data") Map<String, Object> data
    ) {
      this.transactionId = transactionId;
      this.data = data;
    }

    public String getTransactionId() {
      return this.transactionId;
    }

    public Map<String, Object> getData() {
      return this.data;
    }
  }

  private final String INPUT_JSON = a2q("{'b': 2,'a': 1,'transactionId': 'test'," +
      "'c': [{'id': '3','value': 'c'},{'id': '1','value': 'a'},{'id': '2','value': 'b'}]}");

  private final String SECOND_UNEXPECTED_JSON_OUTPUT = a2q("{'transactionId': 'test'," +
      "'c': [{'id': '3','value': 'c'},{'id': '1','value': 'a'},{'id': '2','value': 'b'}]}");

  private final String THIRD_UNEXPECTED_JSON_OUTPUT = a2q("{'transactionId': 'test'}");

  private <T> void testSerializationDeserialization(String outputResult, Class<T> clazz) throws Exception {
    T deserializedObject = objectMapper.readValue(INPUT_JSON, clazz);
    String serializedJson = objectMapper.writeValueAsString(deserializedObject);

    String expectedJson = objectMapper.readTree(outputResult).toPrettyString();
    String actualJson = objectMapper.readTree(serializedJson).toPrettyString();

    assertEquals(expectedJson, actualJson);
  }

  @Test
  public void testSerializationAndDeserializationForFirstObject() throws Exception {
    testSerializationDeserialization(INPUT_JSON, FirstObject.class);
  }

  @Test
  public void testSerializationAndDeserializationForSecondObject() throws Exception {
    testSerializationDeserialization(SECOND_UNEXPECTED_JSON_OUTPUT, SecondObject.class);
  }

  @Test
  public void testSerializationAndDeserializationForThirdObject() throws Exception {
    testSerializationDeserialization(THIRD_UNEXPECTED_JSON_OUTPUT, ThirdObject.class);
  }
}
