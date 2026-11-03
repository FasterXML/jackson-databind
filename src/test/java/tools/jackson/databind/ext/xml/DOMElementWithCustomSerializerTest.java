package tools.jackson.databind.ext.xml;

import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for verifying various issues with custom serializers.
 */
public class DOMElementWithCustomSerializerTest extends DatabindTestUtil
{
    static class ElementSerializer extends StdSerializer<Element>
    {
        public ElementSerializer() { super(Element.class); }
        @Override
        public void serialize(Element value, JsonGenerator g, SerializationContext ctxt) {
            g.writeString("element");
        }
    }

    @JsonSerialize(using = ElementSerializer.class)
    public static class ElementMixin {}

    public static class Immutable {
        protected int x() { return 3; }
        protected int y() { return 7; }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    @Test
    public void testCustomElementSerializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addMixIn(Element.class, ElementMixin.class)
                .build();
        Element element = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument().createElement("el");
        assertEquals("\"element\"", mapper.writeValueAsString(element));
    }
}
