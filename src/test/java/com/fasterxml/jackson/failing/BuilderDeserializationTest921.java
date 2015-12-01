package com.fasterxml.jackson.failing;

import java.util.List;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class BuilderDeserializationTest921
    extends BaseMapTest
{
    public static class MyPOJO {
      public String x;
      public String y;

      @JsonCreator
      public MyPOJO(@JsonProperty("x") String x, @JsonProperty("y") String y) {
        this.x = x;
        this.y = y;
      }
    }

    @JsonDeserialize(builder = MyGenericPOJO.Builder.class)
    public static class MyGenericPOJO<T> {
      private List<T> data;

      private MyGenericPOJO(List<T> d) {
        data = d;
      }

      public List<T> getData() {
        return data;
      }

      public static class Builder<T> {
        private List<T> data;

        public Builder<T> withData(List<T> d) {
          data = d;
          return this;
        }

        public MyGenericPOJO<T> build() {
          return new MyGenericPOJO<T>(data);
        }
      }
    }

    public static class MyGenericPOJOWithCreator<T> {
      private List<T> data;

      private MyGenericPOJOWithCreator(List<T> d) {
          data = d;
      }

      @JsonCreator
      public static <T> MyGenericPOJOWithCreator<T> create(@JsonProperty("data") List<T> data) {
          return new MyGenericPOJOWithCreator.Builder<T>().withData(data).build();
      }

      public List<T> getData() {
          return data;
      }

      public static class Builder<T> {
          private List<T> data;

          public Builder<T> withData(List<T> d) {
              data = d;
              return this;
          }

          public MyGenericPOJOWithCreator<T> build() {
              return new MyGenericPOJOWithCreator<T>(data);
          }
      }
    }

    public void testWithBuilder() throws Exception {
      final ObjectMapper mapper = new ObjectMapper();
      final String json = aposToQuotes("{ 'data': [ { 'x': 'x', 'y': 'y' } ] }");
      final MyGenericPOJO<MyPOJO> deserialized =
          mapper.readValue(json, new TypeReference<MyGenericPOJO<MyPOJO>>() {});
      assertEquals(1, deserialized.data.size());
      Object ob = deserialized.data.get(0);
      assertNotNull(ob);
      assertEquals(MyPOJO.class, ob.getClass());
    }

    public void testWithCreator() throws Exception {
      final ObjectMapper mapper = new ObjectMapper();
      final String json = aposToQuotes("{ 'data': [ { 'x': 'x', 'y': 'y' } ] }");
      final MyGenericPOJOWithCreator<MyPOJO> deserialized =
          mapper.readValue(json,
                  new TypeReference<MyGenericPOJOWithCreator<MyPOJO>>() {});
      assertEquals(1, deserialized.data.size());
      Object ob = deserialized.data.get(0);
      assertNotNull(ob);
      assertEquals(MyPOJO.class, ob.getClass());
    }
  }
