package com.fasterxml.jackson.databind.introspect;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonClassDescription;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BeanDescriptionTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    private final static String CLASS_DESC = "Description, yay!";

    @JsonClassDescription(CLASS_DESC)
    static class DocumentedBean {
        public int x;
    }

    @Test
    void classDesc() throws Exception
    {
        BeanDescription beanDesc = MAPPER.getDeserializationConfig().introspect(MAPPER.constructType(DocumentedBean.class));
        assertEquals(CLASS_DESC, beanDesc.findClassDescription());
    }
}
