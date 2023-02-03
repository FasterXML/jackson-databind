package com.fasterxml.jackson.databind.deser.builder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.LinkedHashMap;
import java.util.List;

// [databind#921]: support infering type parameters from Builder
public class BuilderWithTypeParametersTest
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
      List<T> data;

      MyGenericPOJO(List<T> d) {
        data = d;
      }

      public List<T> getData() {
        return data;
      }

      // 28-Apr-2020, tatu: Note that as per [databind#921] the NAME of
      //   type variable here MUST match that of enclosing class. This has
      //   no semantic meaning to JDK or javac, but internally
      //   `MapperFeature.INFER_BUILDER_TYPE_BINDINGS` relies on this -- but
      //   can not really validate it. So user just has to rely on bit of
      //    black magic to use generic types with builders.
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

    // 05-Sep-2020, tatu: This is not correct and cannot be made to work --
    //   assumption is that static method binding `T` would somehow refer to
    //   class type parameter `T`: this is not true.
/*
    public static class MyGenericPOJOWithCreator<T> {
      List<T> data;

      MyGenericPOJOWithCreator(List<T> d) {
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
    */

    public void testWithBuilderInferringBindings() throws Exception {
        final ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.INFER_BUILDER_TYPE_BINDINGS)
                .build();
        final String json = a2q("{ 'data': [ { 'x': 'x', 'y': 'y' } ] }");
        final MyGenericPOJO<MyPOJO> deserialized =
                mapper.readValue(json, new TypeReference<MyGenericPOJO<MyPOJO>>() {});
        assertEquals(1, deserialized.data.size());
        Object ob = deserialized.data.get(0);
        assertNotNull(ob);
        assertEquals(MyPOJO.class, ob.getClass());
    }

    public void testWithBuilderWithoutInferringBindings() throws Exception {
        final ObjectMapper mapper = jsonMapperBuilder()
                .disable(MapperFeature.INFER_BUILDER_TYPE_BINDINGS)
                .build();
      final String json = a2q("{ 'data': [ { 'x': 'x', 'y': 'y' } ] }");
      final MyGenericPOJO<MyPOJO> deserialized =
          mapper.readValue(json, new TypeReference<MyGenericPOJO<MyPOJO>>() {});
      assertEquals(1, deserialized.data.size());
      Object ob = deserialized.data.get(0);
      assertNotNull(ob);
      assertEquals(LinkedHashMap.class, ob.getClass());
    }

    // 05-Sep-2020, tatu: see above for reason why this can not work
/*
    public void testWithCreator() throws Exception {
      final ObjectMapper mapper = new ObjectMapper();
      final String json = a2q("{ 'data': [ { 'x': 'x', 'y': 'y' } ] }");
      final MyGenericPOJOWithCreator<MyPOJO> deserialized =
          mapper.readValue(json,
                  new TypeReference<MyGenericPOJOWithCreator<MyPOJO>>() {});
      assertEquals(1, deserialized.data.size());
      Object ob = deserialized.data.get(0);
      assertNotNull(ob);
      assertEquals(MyPOJO.class, ob.getClass());
    }
    */
}
