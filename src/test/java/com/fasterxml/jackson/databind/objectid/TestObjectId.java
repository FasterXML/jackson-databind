package com.fasterxml.jackson.databind.objectid;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class TestObjectId extends BaseMapTest
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

    @JsonIdentityInfo(property="id",
            generator=ObjectIdGenerators.PropertyGenerator.class)
    public static class Employee {
        public int id;

        public String name;

        @JsonIdentityReference(alwaysAsId=true)
        public Employee manager;

        @JsonIdentityReference(alwaysAsId=true)
        public List<Employee> reports;

        public Employee() { }
        public Employee(int id, String name, Employee manager) {
            this.id = id;
            this.name = name;
            this.manager = manager;
            reports = new ArrayList<Employee>();
        }

        public Employee addReport(Employee e) {
            reports.add(e);
            return this;
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

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

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

    public void testObjectAndTypeId() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();

        Bar inputRoot = new Bar();
        Foo inputChild = new Foo();
        inputRoot.next = inputChild;
        inputChild.ref = inputRoot;

        String json = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(inputRoot);

        BaseEntity resultRoot = mapper.readValue(json, BaseEntity.class);
        assertNotNull(resultRoot);
        assertTrue(resultRoot instanceof Bar);
        Bar first = (Bar) resultRoot;

        assertNotNull(first.next);
        assertTrue(first.next instanceof Foo);
        Foo second = (Foo) first.next;
        assertNotNull(second.ref);
        assertSame(first, second.ref);
    }

    public static class JsonRoot {
        public final List<JsonSchema> schemas = new ArrayList<JsonSchema>();
    }

    public void testWithFieldsInBaseClass1083() throws Exception {
          final String json = a2q("{'schemas': [{\n"
              + "  'name': 'FoodMart'\n"
              + "}]}\n");
          MAPPER.readValue(json, JsonRoot.class);
    }
}
