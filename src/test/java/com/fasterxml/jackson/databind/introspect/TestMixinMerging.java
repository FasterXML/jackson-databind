package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class TestMixinMerging extends BaseMapTest
{
    public interface Contact {
        String getCity();
    }

    static class ContactImpl implements Contact {
        @Override
        public String getCity() { return "Seattle"; }
    }

    static class ContactMixin implements Contact {
        @Override
        @JsonProperty
        public String getCity() { return null; }
    }

    public interface Person extends Contact {}

    static class PersonImpl extends ContactImpl implements Person {}

    static class PersonMixin extends ContactMixin implements Person {}

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    // for [Issue#515]
    public void testDisappearingMixins515() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
            .disable(MapperFeature.AUTO_DETECT_FIELDS)
            .disable(MapperFeature.AUTO_DETECT_GETTERS)
            .disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
            .disable(MapperFeature.INFER_PROPERTY_MUTATORS);
        SimpleModule module = new SimpleModule("Test");
        module.setMixInAnnotation(Person.class, PersonMixin.class);        
        mapper.registerModule(module);

        assertEquals("{\"city\":\"Seattle\"}", mapper.writeValueAsString(new PersonImpl()));
    }
}
