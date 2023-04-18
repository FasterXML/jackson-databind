package com.fasterxml.jackson.databind.introspect;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.*;

public class IsGetterBooleanTest extends BaseMapTest
{
    // [databind#3609]
    static class POJO3609 {
        int isEnabled;

        protected POJO3609() { }
        public POJO3609(int b) {
            isEnabled = b;
        }

        public int isEnabled() { return isEnabled; }
        public void setEnabled(int b) { isEnabled = b; }
    }

    // [databind#3836]
    static class POJO3836_AR {
        public AtomicReference<Boolean> isAtomic() {
            return new AtomicReference<>(true);
        }
    }

    static class POJO3836_AB {
        public AtomicBoolean isAtomic() {
            return new AtomicBoolean(true);
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    // [databind#3609]
    public void testAllowIntIsGetter() throws Exception
    {
        ObjectMapper MAPPER = jsonMapperBuilder()
                .enable(MapperFeature.ALLOW_IS_GETTERS_FOR_NON_BOOLEAN)
                .build();

        POJO3609 input = new POJO3609(12);
        final String json = MAPPER.writeValueAsString(input);

        Map<?, ?> props = MAPPER.readValue(json, Map.class);
        assertEquals(Collections.singletonMap("enabled", 12),
                props);

        POJO3609 output = MAPPER.readValue(json, POJO3609.class);
        assertEquals(input.isEnabled, output.isEnabled);
    }

    // [databind#3609]
    public void testDisallowIntIsGetter() throws Exception
    {
        ObjectMapper MAPPER = jsonMapperBuilder()
                .disable(MapperFeature.ALLOW_IS_GETTERS_FOR_NON_BOOLEAN)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .build();

        POJO3609 input = new POJO3609(12);
        final String json = MAPPER.writeValueAsString(input);

        assertEquals("{}", json);

    }

    // [databind#3836]
    public void testBooleanReference() throws Exception
    {
        assertEquals(a2q("{'atomic':true}"),
                sharedMapper().writeValueAsString(new POJO3836_AR()));
    }

    // [databind#3836]
    public void testAtomicBoolean() throws Exception
    {
        assertEquals(a2q("{'atomic':true}"),
                sharedMapper().writeValueAsString(new POJO3836_AB()));
    }
}
