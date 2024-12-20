package com.fasterxml.jackson.databind;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

// [databind#4860] ConstructorDetector.USE_PROPERTIES_BASED does not work with multiple constructors since 2.18
public class ConstructorDetector4860Test
        extends DatabindTestUtil
{

    public static class Foo4860 {
        public String id;
        public String name;

        public Foo4860() { }

        public Foo4860(String id) {
            this.id = id;
        }
    }

    @Test
    public void testDeserializationWithEmptyJSON()
            throws Exception
    {
        _testWith(ConstructorDetector.USE_PROPERTIES_BASED);
        _testWith(ConstructorDetector.USE_DELEGATING);
        _testWith(ConstructorDetector.DEFAULT);
        _testWith(ConstructorDetector.EXPLICIT_ONLY);
    }

    private void _testWith(ConstructorDetector detector)
            throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .constructorDetector(detector)
                .build();

        mapper.readValue("{}", Foo4860.class);
        mapper.readValue(a2q("{'id':'something'}"), Foo4860.class);
        mapper.readValue(a2q("{'id':'something','name':'name'}"), Foo4860.class);
    }

}
