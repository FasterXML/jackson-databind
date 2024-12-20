package com.fasterxml.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#4860] ConstructorDetector.USE_PROPERTIES_BASED does not work with multiple constructors since 2.18
public class ConstructorDetector4860Test
    extends DatabindTestUtil
{
    // [databind#4860]
    @JsonPropertyOrder({ "id", "name "})
    static class Foo4860 {
        public String id;
        public String name;

        public Foo4860() { }

        public Foo4860(String id) {
            this.id = id;
        }
    }

    // [databind#4860]
    @Test
    public void testDeserialization4860() throws Exception
    {
        _test4680With(ConstructorDetector.USE_PROPERTIES_BASED);
        _test4680With(ConstructorDetector.USE_DELEGATING);
        _test4680With(ConstructorDetector.DEFAULT);
        _test4680With(ConstructorDetector.EXPLICIT_ONLY);
    }

    private void _test4680With(ConstructorDetector detector) throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .constructorDetector(detector)
                .build();

        
        _test4680With(mapper, "{}", a2q("{'id':null,'name':null}"));
        _test4680With(mapper, a2q("{'id':'something'}"),
                a2q("{'id':'something','name':null}"));
        _test4680With(mapper, a2q("{'id':'something','name':'name'}"),
                a2q("{'id':'something','name':'name'}"));
    }

    private void _test4680With(ObjectMapper mapper, String input, String output) throws Exception
    {
        Foo4860 result = mapper.readValue(input, Foo4860.class);
        assertEquals(output, mapper.writeValueAsString(result));
    }
}
