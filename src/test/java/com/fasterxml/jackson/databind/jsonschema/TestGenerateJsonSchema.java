package com.fasterxml.jackson.databind.jsonschema;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ryan Heaton
 */
@SuppressWarnings("deprecation")
public class TestGenerateJsonSchema
    extends DatabindTestUtil
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
        @JsonProperty(required=true)
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

        public void setProperty5(String property5) {
            this.property5 = property5;
        }

        public long getProperty6() {
            return 0L;
        }
    }

    public class TrivialBean {
        public String name;
    }

    @JsonSerializableSchema(id="myType")
    public class BeanWithId {
        public String value;
    }

    static class UnwrappingRoot
    {
        public int age;

        @JsonUnwrapped(prefix="name.")
        public Name name;
    }

    static class Name {
        public String first, last;
    }

    @JsonPropertyOrder({ "dec", "bigInt" })
    static class Numbers {
        public BigDecimal dec;
        public BigInteger bigInt;
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    /**
     * tests generating json-schema stuff.
     */
    @Test
    public void testOldSchemaGeneration() throws Exception
    {
        JsonSchema jsonSchema = MAPPER.generateJsonSchema(SimpleBean.class);

        assertNotNull(jsonSchema);

        // test basic equality, and that equals() handles null, other obs
        assertTrue(jsonSchema.equals(jsonSchema));
        assertFalse(jsonSchema.equals(null));
        assertFalse(jsonSchema.equals("foo"));

        // other basic things
        assertNotNull(jsonSchema.toString());
        assertNotNull(JsonSchema.getDefaultSchemaNode());

        ObjectNode root = jsonSchema.getSchemaNode();
        assertEquals("object", root.get("type").asText());
        assertFalse(root.path("required").booleanValue());
        JsonNode propertiesSchema = root.get("properties");
        assertNotNull(propertiesSchema);
        JsonNode property1Schema = propertiesSchema.get("property1");
        assertNotNull(property1Schema);
        assertEquals("integer", property1Schema.get("type").asText());
        assertFalse(property1Schema.path("required").booleanValue());
        JsonNode property2Schema = propertiesSchema.get("property2");
        assertNotNull(property2Schema);
        assertEquals("string", property2Schema.get("type").asText());
        assertFalse(property2Schema.path("required").booleanValue());
        JsonNode property3Schema = propertiesSchema.get("property3");
        assertNotNull(property3Schema);
        assertEquals("array", property3Schema.get("type").asText());
        assertFalse(property3Schema.path("required").booleanValue());
        assertEquals("string", property3Schema.get("items").get("type").asText());
        JsonNode property4Schema = propertiesSchema.get("property4");
        assertNotNull(property4Schema);
        assertEquals("array", property4Schema.get("type").asText());
        assertFalse(property4Schema.path("required").booleanValue());
        assertEquals("number", property4Schema.get("items").get("type").asText());

        JsonNode property5Schema = propertiesSchema.get("property5");
        assertNotNull(property5Schema);
        assertEquals("string", property5Schema.get("type").asText());
        assertTrue(property5Schema.path("required").booleanValue());

        JsonNode property6Schema = propertiesSchema.get("property6");
        assertNotNull(property6Schema);
        assertEquals("integer", property6Schema.get("type").asText());
        assertFalse(property6Schema.path("required").booleanValue());
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

    final static FilterProvider secretFilterProvider = new SimpleFilterProvider()
        .addFilter("filteredBean", SimpleBeanPropertyFilter.filterOutAllExcept(new String[]{"obvious"}));

    @Test
    public void testGeneratingJsonSchemaWithFilters() throws Exception {
    	ObjectMapper mapper = new ObjectMapper();
    	mapper.setFilters(secretFilterProvider);
    	JsonSchema schema = mapper.generateJsonSchema(FilteredBean.class);
    	JsonNode node = schema.getSchemaNode().get("properties");
    	assertTrue(node.has("obvious"));
    	assertFalse(node.has("secret"));
    }

    /**
     * Additional unit test for verifying that schema object itself
     * can be properly serialized
     */
    @Test
    public void testSchemaSerialization() throws Exception
    {
        JsonSchema jsonSchema = MAPPER.generateJsonSchema(SimpleBean.class);
        Map<String,Object> result = writeAndMap(MAPPER, jsonSchema);
        assertNotNull(result);
        // no need to check out full structure, just basics...
        assertEquals("object", result.get("type"));
        // only add 'required' if it is true...
        assertNull(result.get("required"));
        assertNotNull(result.get("properties"));
    }

    @Test
    public void testThatObjectsHaveNoItems() throws Exception
    {
        JsonSchema jsonSchema = MAPPER.generateJsonSchema(TrivialBean.class);
        String json = jsonSchema.toString().replaceAll("\"", "'");
        // can we count on ordering being stable? I think this is true with current ObjectNode impl
        // as perh [JACKSON-563]; 'required' is only included if true
        assertEquals("{'type':'object','properties':{'name':{'type':'string'}}}",
                json);
    }

    @Test
    public void testSchemaId() throws Exception
    {
        JsonSchema jsonSchema = MAPPER.generateJsonSchema(BeanWithId.class);
        String json = jsonSchema.toString().replaceAll("\"", "'");
        assertEquals("{'type':'object','id':'myType','properties':{'value':{'type':'string'}}}",
                json);
    }

    // [databind#271]
    @Test
    public void testUnwrapping()  throws Exception
    {
        JsonSchema jsonSchema = MAPPER.generateJsonSchema(UnwrappingRoot.class);
        ObjectNode root = jsonSchema.getSchemaNode();
        JsonNode propertiesSchema = root.get("properties");
        String ageType = propertiesSchema.get("age").get("type").asText();
        String firstType = propertiesSchema.get("name.first").get("type").asText();
        String lastType = propertiesSchema.get("name.last").get("type").asText();
        String type = root.get("type").asText();
        assertEquals(type, "object");
        assertEquals(ageType, "integer");
        assertEquals(firstType, "string");
        assertEquals(lastType, "string");
    }

    //
    @Test
    public void testNumberTypes()  throws Exception
    {
        JsonSchema jsonSchema = MAPPER.generateJsonSchema(Numbers.class);
        String json = quotesToApos(jsonSchema.toString());
        String EXP = "{'type':'object',"
                +"'properties':{'dec':{'type':'number'},"
                +"'bigInt':{'type':'integer'}}}";
        assertEquals(EXP, json);
    }

    protected static String quotesToApos(String json) {
        return json.replace("\"", "'");
    }
}
