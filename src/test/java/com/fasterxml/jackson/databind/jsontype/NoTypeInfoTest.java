package com.fasterxml.jackson.databind.jsontype;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class NoTypeInfoTest extends DatabindTestUtil
{
    @JsonTypeInfo(use=JsonTypeInfo.Id.NONE)
    @JsonDeserialize(as=NoType.class)
    static interface NoTypeInterface {
    }

    final static class NoType implements NoTypeInterface {
        public int a = 3;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    @Test
    public void testWithIdNone() throws Exception
    {
        final ObjectMapper mapper = jsonMapperBuilder()
            .activateDefaultTyping(NoCheckSubTypeValidator.instance)
            .build();
        // serialize without type info
        String json = mapper.writeValueAsString(new NoType());
        assertEquals("{\"a\":3}", json);

        // and deserialize successfully
        NoTypeInterface bean = mapper.readValue("{\"a\":6}", NoTypeInterface.class);
        assertNotNull(bean);
        NoType impl = (NoType) bean;
        assertEquals(6, impl.a);
    }
}
