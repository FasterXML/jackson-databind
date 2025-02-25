package com.fasterxml.jackson.databind.objectid;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class TestObjectId extends DatabindTestUtil
{
    @JsonPropertyOrder({"a", "b"})
    static class Wrapper {
        public ColumnMetadata a, b;
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
    static class ColumnMetadata {
      private final String name;
      private final String type;
      private final String comment;

      @JsonCreator
      public ColumnMetadata(
        @JsonProperty("name") String name,
        @JsonProperty("type") String type,
        @JsonProperty("comment") String comment
      ) {
        this.name = name;
        this.type = type;
        this.comment = comment;
      }

      @JsonProperty("name")
      public String getName() {
        return name;
      }

      @JsonProperty("type")
      public String getType() {
        return type;
      }

      @JsonProperty("comment")
      public String getComment() {
        return comment;
      }
    }

    /* Problem in which always-as-id reference may prevent initial
     * serialization of a POJO.
     */

    static class Company {
        public List<Employee> employees;

        public void add(Employee e) {
            if (employees == null) {
                employees = new ArrayList<Employee>();
            }
            employees.add(e);
        }
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
    public static class BaseEntity {  }

    public static class Foo extends BaseEntity {
        public BaseEntity ref;
    }

    public static class Bar extends BaseEntity
    {
        public Foo next;
    }

    // for [databind#1083]
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "type",
            defaultImpl = JsonMapSchema.class)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = JsonMapSchema.class, name = "map"),
        @JsonSubTypes.Type(value = JsonJdbcSchema.class, name = "jdbc") })
    public static abstract class JsonSchema {
        public String name;
    }

    static class JsonMapSchema extends JsonSchema { }

    static class JsonJdbcSchema extends JsonSchema { }

    static class JsonRoot1083 {
        public List<JsonSchema> schemas = new ArrayList<JsonSchema>();
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testColumnMetadata() throws Exception
    {
        ColumnMetadata col = new ColumnMetadata("Billy", "employee", "comment");
        Wrapper w = new Wrapper();
        w.a = col;
        w.b = col;
        String json = MAPPER.writeValueAsString(w);

        Wrapper deserialized = MAPPER.readValue(json, Wrapper.class);
        assertNotNull(deserialized);
        assertNotNull(deserialized.a);
        assertNotNull(deserialized.b);

        assertEquals("Billy", deserialized.a.getName());
        assertEquals("employee", deserialized.a.getType());
        assertEquals("comment", deserialized.a.getComment());

        assertSame(deserialized.a, deserialized.b);
    }

    // For [databind#188]
    @Test
    public void testMixedRefsIssue188() throws Exception
    {
        Company comp = new Company();
        Employee e1 = new Employee(1, "First", null);
        Employee e2 = new Employee(2, "Second", e1);
        e1.addReport(e2);
        comp.add(e1);
        comp.add(e2);

        JsonMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
        String json = mapper.writeValueAsString(comp);

        assertEquals("{\"employees\":["
                +"{\"id\":1,\"manager\":null,\"name\":\"First\",\"reports\":[2]},"
                +"{\"id\":2,\"manager\":1,\"name\":\"Second\",\"reports\":[]}"
                +"]}",
                json);
    }

    @Test
    public void testObjectAndTypeId() throws Exception
    {
        Bar inputRoot = new Bar();
        Foo inputChild = new Foo();
        inputRoot.next = inputChild;
        inputChild.ref = inputRoot;

        String json = MAPPER.writerWithDefaultPrettyPrinter()
                .writeValueAsString(inputRoot);

        BaseEntity resultRoot = MAPPER.readValue(json, BaseEntity.class);
        assertNotNull(resultRoot);
        assertTrue(resultRoot instanceof Bar);
        Bar first = (Bar) resultRoot;

        assertNotNull(first.next);
        assertTrue(first.next instanceof Foo);
        Foo second = (Foo) first.next;
        assertNotNull(second.ref);
        assertSame(first, second.ref);
    }

    @Test
    public void testWithFieldsInBaseClass1083() throws Exception {
          final String json = a2q("{'schemas': [{\n"
              + "  'name': 'FoodMart'\n"
              + "}]}\n");
          MAPPER.readValue(json, JsonRoot1083.class);
    }
}
