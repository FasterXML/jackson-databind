package com.fasterxml.jackson.databind.mixins;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class TestMixinMerging extends DatabindTestUtil
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

    // for [databind#515]
    @Test
    public void testDisappearingMixins515() throws Exception
    {
        SimpleModule module = new SimpleModule("Test");
        module.setMixInAnnotation(Person.class, PersonMixin.class);
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
                .disable(MapperFeature.AUTO_DETECT_FIELDS)
                .disable(MapperFeature.AUTO_DETECT_GETTERS)
                .disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
                .disable(MapperFeature.INFER_PROPERTY_MUTATORS)
                .addModule(module)
                .build();
        assertEquals("{\"city\":\"Seattle\"}", mapper.writeValueAsString(new PersonImpl()));
    }
}
