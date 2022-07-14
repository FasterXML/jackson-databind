package tools.jackson.databind.deser.jdk;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

// [databind#2675]: Void-valued "properties"
public class VoidProperties2675Test extends BaseMapTest
{
    static class VoidBean {
        protected Void value;

        public Void getValue() { return null; }

//        public void setValue(Void v) { }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper VOID_MAPPER = sharedMapper();

    private final ObjectMapper NO_VOID_MAPPER = jsonMapperBuilder()
            .disable(MapperFeature.ALLOW_VOID_VALUED_PROPERTIES)
            .build();

    public void testVoidBeanSerialization() throws Exception
    {
        // with 3.x enabled by default, but may disable
        assertEquals("{\"value\":null}", VOID_MAPPER.writeValueAsString(new VoidBean()));
        try {
            NO_VOID_MAPPER.writeValueAsString(new VoidBean());
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "no properties discovered");
        }
    }

    public void testVoidBeanDeserialization() throws Exception {
        final String DOC = "{\"value\":null}";
        VoidBean result = VOID_MAPPER.readValue(DOC, VoidBean.class);
        assertNotNull(result);
        assertNull(result.getValue());

        // By default (2.x), not enabled:
        try {
            result = NO_VOID_MAPPER.readValue(DOC, VoidBean.class);
            fail("Should not pass");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized property \"value\"");
        }
    }
}
