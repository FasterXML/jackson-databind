package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class ExternalTypeIdWithUnwrapped2039Test extends BaseMapTest
{
    static class MainType2039 {
        public String text;

        @JsonUnwrapped public Wrapped2039 wrapped;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "subtype")
        @JsonSubTypes({ @JsonSubTypes.Type(value = SubA2039.class, name = "SubA") })
        public SubType2039 sub;

        public void setSub(SubType2039 s) {
            sub = s;
        }

        public void setWrapped(Wrapped2039 w) {
            wrapped = w;
        }
    }

    static class Wrapped2039 {
        public String wrapped;
    }

    public static class SubType2039 { }

    public static class SubA2039 extends SubType2039 {
        @JsonProperty public boolean bool;
    }

    public void testExternalWithUnwrapped2039() throws Exception
    {
        final ObjectMapper mapper = newJsonMapper();

        final String json = a2q("{\n"
                +"'text': 'this is A',\n"
                +"'wrapped': 'yes',\n"
                +"'subtype': 'SubA',\n"
                +"'sub': {\n"
                +"  'bool': true\n"
                +"}\n"
                +"}");
        final MainType2039 main = mapper.readValue(json, MainType2039.class);

        assertEquals("this is A", main.text);
        assertEquals("yes", main.wrapped.wrapped);

        assertNotNull(main.sub);
        assertEquals(SubA2039.class, main.sub.getClass()); // <- fails here
        assertEquals(true, ((SubA2039) main.sub).bool);
    }
}
