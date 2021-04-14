package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

// for [databind#1391]: should allow disabling of default typing
// via explicit {@link JsonTypeInfo}
public class DefaultTypingOverride1391Test extends BaseMapTest
{
    static class ListWrapper {
        /* 03-Oct-2016, tatu: This doesn't work because it applies to contents
         *   (elements), NOT the container. But there is no current mechanism
         *   to change that; need to add a new feature or properties in 2.9
         */
        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        public Collection<String> stuff = Collections.emptyList();
    }

    public void testCollectionWithOverride() throws Exception
    {
        final ObjectMapper mapper = JsonMapper.builder()
            .activateDefaultTypingAsProperty(NoCheckSubTypeValidator.instance,
                    ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE,
                    "$type")
            .build();
        String json = mapper.writeValueAsString(new ListWrapper());
        assertEquals(a2q("{'stuff':[]}"), json);
    }
}
