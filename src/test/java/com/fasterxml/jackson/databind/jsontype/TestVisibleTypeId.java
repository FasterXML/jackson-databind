package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import com.fasterxml.jackson.databind.*;

/**
 * Tests to verify [JACKSON-437], [JACKSON-762]
 */
public class TestVisibleTypeId extends BaseMapTest
{
    // type id as property, exposed
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY,
            property="type", visible=true)
    @JsonTypeName("BaseType")
    static class PropertyBean {
        public int a = 3;

        protected String type;

        public void setType(String t) { type = t; }
    }

    // as wrapper-array
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.WRAPPER_ARRAY,
            property="type", visible=true)
    @JsonTypeName("ArrayType")
    static class WrapperArrayBean {
        public int a = 1;

        protected String type;

        public void setType(String t) { type = t; }
    }

    // as wrapper-object
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.WRAPPER_OBJECT,
            property="type", visible=true)
    @JsonTypeName("ObjectType")
    static class WrapperObjectBean {
        public int a = 2;

        protected String type;

        public void setType(String t) { type = t; }
    }

    // as external id, bit trickier
    static class ExternalIdWrapper {
        @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property="type", visible=true)
        public ExternalIdBean bean = new ExternalIdBean();
    }
    
    @JsonTypeName("ExternalType")
    static class ExternalIdBean {
        public int a = 2;

        protected String type;

        public void setType(String t) { type = t; }
    }

    // // // [JACKSON-762]: type id from property
    
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY,
            property="type")
    static class TypeIdFromFieldProperty {
        public int a = 3;

        @JsonTypeId
        public String type = "SomeType";
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.WRAPPER_ARRAY,
            property="type")
    static class TypeIdFromFieldArray {
        public int a = 3;
        @JsonTypeId
        public String type = "SomeType";
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.WRAPPER_OBJECT,
            property="type")
    static class TypeIdFromMethodObject {
        public int a = 3;
        
        @JsonTypeId
        public String getType() { return "SomeType"; }
    }

    static class ExternalIdWrapper2 {
        @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property="type", visible=true)
        public ExternalIdBean2 bean = new ExternalIdBean2();
    }

    static class ExternalIdBean2 {
        public int a = 2;

        /* Type id property itself can not be external, as it is conceptually
         * part of the bean for which info is written:
         */
        @JsonTypeId
        public String getType() { return "SomeType"; }
    }

    // Invalid definition: multiple type ids
    static class MultipleIds {
        @JsonTypeId
        public String type1 = "type1";

        @JsonTypeId
        public String getType2() { return "type2"; };
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper mapper = new ObjectMapper();
    
    public void testVisibleWithProperty() throws Exception
    {
        String json = mapper.writeValueAsString(new PropertyBean());
        // just default behavior:
        assertEquals("{\"type\":\"BaseType\",\"a\":3}", json);
        // but then expect to read it back
        PropertyBean result = mapper.readValue(json, PropertyBean.class);
        assertEquals("BaseType", result.type);

        // also, should work with order reversed
        result = mapper.readValue("{\"a\":7, \"type\":\"BaseType\"}", PropertyBean.class);
        assertEquals(7, result.a);
        assertEquals("BaseType", result.type);
    }

    public void testVisibleWithWrapperArray() throws Exception
    {
        String json = mapper.writeValueAsString(new WrapperArrayBean());
        // just default behavior:
        assertEquals("[\"ArrayType\",{\"a\":1}]", json);
        // but then expect to read it back
        WrapperArrayBean result = mapper.readValue(json, WrapperArrayBean.class);
        assertEquals("ArrayType", result.type);
        assertEquals(1, result.a);
    }

    public void testVisibleWithWrapperObject() throws Exception
    {
        String json = mapper.writeValueAsString(new WrapperObjectBean());
        assertEquals("{\"ObjectType\":{\"a\":2}}", json);
        // but then expect to read it back
        WrapperObjectBean result = mapper.readValue(json, WrapperObjectBean.class);
        assertEquals("ObjectType", result.type);
    }

    public void testVisibleWithExternalId() throws Exception
    {
        String json = mapper.writeValueAsString(new ExternalIdWrapper());
        // but then expect to read it back
        ExternalIdWrapper result = mapper.readValue(json, ExternalIdWrapper.class);
        assertEquals("ExternalType", result.bean.type);
        assertEquals(2, result.bean.a);
    }

    // [JACKSON-762]

    public void testTypeIdFromProperty() throws Exception
    {
        assertEquals("{\"type\":\"SomeType\",\"a\":3}",
                mapper.writeValueAsString(new TypeIdFromFieldProperty()));
    }

    public void testTypeIdFromArray() throws Exception
    {
        assertEquals("[\"SomeType\",{\"a\":3}]",
                mapper.writeValueAsString(new TypeIdFromFieldArray()));
    }

    public void testTypeIdFromObject() throws Exception
    {
        assertEquals("{\"SomeType\":{\"a\":3}}",
                mapper.writeValueAsString(new TypeIdFromMethodObject()));
    }

    public void testTypeIdFromExternal() throws Exception
    {
        String json = mapper.writeValueAsString(new ExternalIdWrapper2());
        // Implementation detail: type id written AFTER value, due to constraints
        assertEquals("{\"bean\":{\"a\":2},\"type\":\"SomeType\"}", json);
        
    }

    // Failing cases

    public void testInvalidMultipleTypeIds() throws Exception
    {
        try {
            mapper.writeValueAsString(new MultipleIds());
            fail("Should have failed");
        } catch (JsonMappingException e) {
            verifyException(e, "multiple type ids");
        }
    }
}
