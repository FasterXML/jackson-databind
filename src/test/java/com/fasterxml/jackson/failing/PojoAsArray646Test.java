package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class PojoAsArray646Test extends BaseMapTest
{
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder(alphabetic = true)
    static class Outer {

        private Map<String, TheItem> attributes;

        public Outer() {
            attributes = new HashMap<String, TheItem>();
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.WRAPPER_ARRAY)
        @JsonTypeIdResolver(DmTypeIdResolver.class)
        public Map<String, TheItem> getAttributes() {
            return attributes;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder(alphabetic = true)
    static class TheBean {
        private List<TheItem> items;

        @JsonCreator
        public TheBean(@JsonProperty("items") List<TheItem> items) {
            this.items = items;
        }


        public List<TheItem> getItems() {
            return items;
        }

        public void setItems(List<TheItem> items) {
            this.items = items;
        }
    }
    
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder(alphabetic = true)
    static class TheItem {

        @JsonFormat(shape = JsonFormat.Shape.ARRAY)
        @JsonPropertyOrder(alphabetic = true)
        public static class NestedItem {
            private String nestedStrValue;

            @JsonCreator
            public NestedItem(@JsonProperty("nestedStrValue") String nestedStrValue) {
                this.nestedStrValue = nestedStrValue;
            }

            public String getNestedStrValue() {
                return nestedStrValue;
            }

            public void setNestedStrValue(String nestedStrValue) {
                this.nestedStrValue = nestedStrValue;
            }
        }

        private String strValue;
        private boolean boolValue;
        private List<NestedItem> nestedItems;

        @JsonCreator
        public TheItem(@JsonProperty("strValue") String strValue, @JsonProperty("boolValue") boolean boolValue, @JsonProperty("nestedItems") List<NestedItem> nestedItems) {
            this.strValue = strValue;
            this.boolValue = boolValue;
            this.nestedItems = nestedItems;
        }

        public String getStrValue() {
            return strValue;
        }

        public void setStrValue(String strValue) {
            this.strValue = strValue;
        }

        public boolean isBoolValue() {
            return boolValue;
        }

        public void setBoolValue(boolean boolValue) {
            this.boolValue = boolValue;
        }

        public List<NestedItem> getNestedItems() {
            return nestedItems;
        }

        public void setNestedItems(List<NestedItem> nestedItems) {
            this.nestedItems = nestedItems;
        }
    }

    static class DmTypeIdResolver implements TypeIdResolver {

        @Override
        public void init(JavaType javaType) { }

        @Override
        public String idFromValue(Object o) {
            return idFromValueAndType(o, o.getClass());
        }

        @Override
        public String idFromValueAndType(Object o, Class<?> aClass) {
            return o.getClass().getName();
        }

        @Override
        public String idFromBaseType() {
            throw new RuntimeException("Unsupported serialization case");
        }

        @Override
        public JavaType typeFromId(String key) {
            Class<?> clazz ;
            try {
                clazz = getClass().getClassLoader().loadClass(key);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            return TypeFactory.defaultInstance().constructType(clazz);
        }

        @Override
        public JsonTypeInfo.Id getMechanism() {
            return JsonTypeInfo.Id.CUSTOM;
        }

        @Override
        public JavaType typeFromId(DatabindContext context, String id) {
            return typeFromId(id);
        }
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */
    
    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testWithCustomTypeId() throws Exception {

        List<TheItem.NestedItem> nestedList = new ArrayList<TheItem.NestedItem>();
        nestedList.add(new TheItem.NestedItem("foo1"));
        nestedList.add(new TheItem.NestedItem("foo2"));
        TheItem item = new TheItem("first", false, nestedList);
        Outer outer = new Outer();
        outer.getAttributes().put(TheItem.class.getName(), item);

        String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(outer);
        System.out.println(json);

        Outer result = MAPPER.readValue(json, Outer.class);
        assertNotNull(result);
    }
}
