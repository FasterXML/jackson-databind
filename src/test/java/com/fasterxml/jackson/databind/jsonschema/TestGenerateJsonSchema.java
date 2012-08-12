package com.fasterxml.jackson.databind.jsonschema;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonschema.factories.SchemaFactory;
import com.fasterxml.jackson.databind.jsonschema.factories.SchemaFactoryProvider;
import com.fasterxml.jackson.databind.jsonschema.types.ObjectSchema;
import com.fasterxml.jackson.databind.jsonschema.types.JsonSchema;
import com.fasterxml.jackson.databind.jsonschema.types.ArraySchema.Items;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

/**
 * @author Ryan Heaton
 */
public class TestGenerateJsonSchema
    extends com.fasterxml.jackson.databind.BaseMapTest
{
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    public static class SimpleBean
    {
        private int property1;
        private String property2;
        private String[] property3;
        private Collection<Float> property4;
        @JsonProperty(required = true)
        private String property5;
        
        public int getProperty1()
        {
            return property1;
        }

        public void setProperty1(int property1)
        {
            this.property1 = property1;
        }

        public String getProperty2()
        {
            return property2;
        }

        public void setProperty2(String property2)
        {
            this.property2 = property2;
        }

        public String[] getProperty3()
        {
            return property3;
        }

        public void setProperty3(String[] property3)
        {
            this.property3 = property3;
        }

        public Collection<Float> getProperty4()
        {
            return property4;
        }

        public void setProperty4(Collection<Float> property4)
        {
            this.property4 = property4;
        }
        
        public String getProperty5()
        {
            return property5;
        }

        public void setProperty5(String property5)
        {
            this.property5 = property5;
        }
    }

    public class TrivialBean {
        public String name;
    }

    //@JsonSerializableSchema(id="myType")
    public class BeanWithId {
        public String value;
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();
    
    /**
     * tests generating json-schema stuff.
     */
    public void testGeneratingJsonSchema()
        throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        SchemaFactoryProvider visitor = new SchemaFactoryProvider();
        m.acceptJsonFormatVisitor(SimpleBean.class, visitor);
        JsonSchema jsonSchema = visitor.finalSchema();
        
        assertNotNull(jsonSchema);

        // test basic equality, and that equals() handles null, other obs
        assertTrue(jsonSchema.equals(jsonSchema));
        assertFalse(jsonSchema.equals(null));
        assertFalse(jsonSchema.equals("foo"));

        assertTrue(jsonSchema.isObjectSchema());
        ObjectSchema object = jsonSchema.asObjectSchema();
        assertNotNull(object);
        Map<String,JsonSchema> properties = object.getProperties();
        assertNotNull(properties);
        JsonSchema prop1 = properties.get("property1");
        assertNotNull(prop1);
        assertTrue(prop1.isIntegerSchema());
        assertNull(prop1.getRequired());
        
        JsonSchema prop2 = properties.get("property2");
        assertNotNull(prop2);
        assertTrue(prop2.isStringSchema());
        assertNull(prop2.getRequired());
        
        JsonSchema prop3 = properties.get("property3");
        assertNotNull(prop3);
        assertTrue(prop3.isArraySchema());
        assertNull(prop3.getRequired());
        Items items = prop3.asArraySchema().getItems();
        assertTrue(items.isSingleItems());
        JsonSchema itemType = items.asSingleItems().getSchema();
        assertNotNull(itemType);
        assertTrue(itemType.isStringSchema());
        
        JsonSchema prop4 = properties.get("property4");
        assertNotNull(prop4);
        assertTrue(prop4.isArraySchema());
        assertNull(prop4.getRequired());
        items = prop4.asArraySchema().getItems();
        assertTrue(items.isSingleItems());
        itemType = items.asSingleItems().getSchema();
        assertNotNull(itemType);
        assertTrue(itemType.isNumberSchema());
        
        JsonSchema prop5 = properties.get("property5");
        assertNotNull(prop5);
        assertTrue(prop5.getRequired());
      
    }
    
    @JsonFilter("filteredBean")
    protected static class FilteredBean {
    	
    	@JsonProperty
    	private String secret = "secret";
    	
    	@JsonProperty
    	private String obvious = "obvious";
    	
    	public String getSecret() { return secret; }
    	public void setSecret(String s) { secret = s; }
    	
    	public String getObvious() { return obvious; }
    	public void setObvious(String s) {obvious = s; }
    }
    
    public static FilterProvider secretFilterProvider = new SimpleFilterProvider()
        .addFilter("filteredBean", SimpleBeanPropertyFilter.filterOutAllExcept(new String[]{"obvious"}));

    public void testGeneratingJsonSchemaWithFilters() throws Exception {
    	ObjectMapper mapper = new ObjectMapper();
    	mapper.setFilters(secretFilterProvider);
    	SchemaFactoryProvider visitor = new SchemaFactoryProvider();
        mapper.acceptJsonFormatVisitor(FilteredBean.class, visitor);
        JsonSchema jsonSchema = visitor.finalSchema();
    	assertNotNull(jsonSchema);
    	assertTrue(jsonSchema.isObjectSchema());
    	ObjectSchema object = jsonSchema.asObjectSchema();
    	assertNotNull(object);
    	Map<String, JsonSchema> properties = object.getProperties();
    	assertNotNull(properties);
    	JsonSchema obvious = properties.get("obvious");
    	assertNotNull(obvious);
    	assertTrue(obvious.isStringSchema());
    	assertNull(properties.get("secret"));
    }

    /**
     * Additional unit test for verifying that schema object itself
     * can be properly serialized
     */
    public void testSchemaSerialization()
            throws Exception
    {
    	SchemaFactoryProvider visitor = new SchemaFactoryProvider();
        MAPPER.acceptJsonFormatVisitor(SimpleBean.class, visitor);
        JsonSchema jsonSchema = visitor.finalSchema();
        Map<String,Object> result = writeAndMap(MAPPER, jsonSchema);
        assertNotNull(result);
        // no need to check out full structure, just basics...
        assertEquals("object", result.get("type"));
        // only add 'required' if it is true...
        assertNull(result.get("required"));
        assertNotNull(result.get("properties"));
    }

    public void testInvalidCall()
        throws Exception
    {
        // not ok to pass null
        try {
        	SchemaFactoryProvider visitor = new SchemaFactoryProvider();
            MAPPER.acceptJsonFormatVisitor(null, visitor);
            JsonSchema jsonSchema = visitor.finalSchema();
            fail("Should have failed");
        } catch (IllegalArgumentException iae) {
            verifyException(iae, "class must be provided");
        }
    }

    /**
     * Test for [JACKSON-454]
     */
    public void testThatObjectsHaveNoItems() throws Exception
    {
    	SchemaFactoryProvider visitor = new SchemaFactoryProvider();
        MAPPER.acceptJsonFormatVisitor(TrivialBean.class, visitor);
        JsonSchema jsonSchema = visitor.finalSchema();
        Map<String,Object> result = writeAndMap(MAPPER, jsonSchema);
        // can we count on ordering being stable? I think this is true with current ObjectNode impl
        // as perh [JACKSON-563]; 'required' is only included if true
        assertFalse(result.containsKey("items"));

    }

    @SuppressWarnings({ "unchecked", "rawtypes", "serial" })
	public void testSchemaId() throws Exception
    {
    	SchemaFactoryProvider visitor = new SchemaFactoryProvider();
        MAPPER.acceptJsonFormatVisitor(BeanWithId.class, visitor);
        JsonSchema jsonSchema = visitor.finalSchema();
        Map<String,Object> result = writeAndMap(MAPPER, jsonSchema);
        
        assertEquals(new HashMap() {{ 
        	put("type", "object");
        	put("properties", 
        			new HashMap(){{ put("value", 
        					new HashMap() {{ put("type", "string");}}
        			);}}
        	);}}, result);
    }
}
