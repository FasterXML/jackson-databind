package tools.jackson.databind.introspect;

import java.io.StringWriter;
import java.util.*;

import javax.xml.namespace.QName;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.deser.std.StdDeserializer;

public class TestJacksonAnnotationIntrospector
    extends BaseMapTest
{
    public static enum EnumExample {
        VALUE1;
    }

    public static class JacksonExample
    {
        protected String attributeProperty;
        protected String elementProperty;
        protected List<String> wrappedElementProperty;
        protected EnumExample enumProperty;
        protected QName qname;

        @JsonSerialize(using=QNameSerializer.class)
        public QName getQname()
        {
            return qname;
        }

        @JsonDeserialize(using=QNameDeserializer.class)
        public void setQname(QName qname)
        {
            this.qname = qname;
        }

        @JsonProperty("myattribute")
        public String getAttributeProperty()
        {
            return attributeProperty;
        }

        @JsonProperty("myattribute")
        public void setAttributeProperty(String attributeProperty)
        {
            this.attributeProperty = attributeProperty;
        }

        @JsonProperty("myelement")
        public String getElementProperty()
        {
            return elementProperty;
        }

        @JsonProperty("myelement")
        public void setElementProperty(String elementProperty)
        {
            this.elementProperty = elementProperty;
        }

        @JsonProperty("mywrapped")
        public List<String> getWrappedElementProperty()
        {
            return wrappedElementProperty;
        }

        @JsonProperty("mywrapped")
        public void setWrappedElementProperty(List<String> wrappedElementProperty)
        {
            this.wrappedElementProperty = wrappedElementProperty;
        }

        public EnumExample getEnumProperty()
        {
            return enumProperty;
        }

        public void setEnumProperty(EnumExample enumProperty)
        {
            this.enumProperty = enumProperty;
        }
    }

    public static class QNameSerializer extends ValueSerializer<QName> {

        @Override
        public void serialize(QName value, JsonGenerator g, SerializerProvider provider)
        {
            g.writeString(value.toString());
        }
    }


    public static class QNameDeserializer extends StdDeserializer<QName>
    {
        public QNameDeserializer() { super(QName.class); }
        @Override
        public QName deserialize(JsonParser p, DeserializationContext ctxt)
        {
            return QName.valueOf(p.readValueAs(String.class));
        }
    }

    @JsonIgnoreType
    static class IgnoredType { }

    static class IgnoredSubType extends IgnoredType { }

    // Test to ensure we can override enum settings
    static class LcEnumIntrospector extends JacksonAnnotationIntrospector
    {
        private static final long serialVersionUID = 1L;

        @Override
        public  String[] findEnumValues(MapperConfig<?> config,
                Class<?> enumType, Enum<?>[] enumValues, String[] names) {
            // kinda sorta wrong, but for testing's sake...
            for (int i = 0, len = enumValues.length; i < len; ++i) {
                names[i] = enumValues[i].name().toLowerCase();
            }
            return names;
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    /**
     * tests getting serializer/deserializer instances.
     */
    public void testSerializeDeserializeWithJaxbAnnotations() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
        JacksonExample ex = new JacksonExample();
        QName qname = new QName("urn:hi", "hello");
        ex.setQname(qname);
        ex.setAttributeProperty("attributeValue");
        ex.setElementProperty("elementValue");
        ex.setWrappedElementProperty(Arrays.asList("wrappedElementValue"));
        ex.setEnumProperty(EnumExample.VALUE1);
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, ex);
        writer.flush();
        writer.close();

        String json = writer.toString();
        JacksonExample readEx = mapper.readValue(json, JacksonExample.class);

        assertEquals(ex.qname, readEx.qname);
        assertEquals(ex.attributeProperty, readEx.attributeProperty);
        assertEquals(ex.elementProperty, readEx.elementProperty);
        assertEquals(ex.wrappedElementProperty, readEx.wrappedElementProperty);
        assertEquals(ex.enumProperty, readEx.enumProperty);
    }

    public void testEnumHandling() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .annotationIntrospector(new LcEnumIntrospector())
                .build();
        assertEquals("\"value1\"", mapper.writeValueAsString(EnumExample.VALUE1));
        EnumExample result = mapper.readValue(q("value1"), EnumExample.class);
        assertEquals(EnumExample.VALUE1, result);
    }
}
