package com.fasterxml.jackson.failing;

import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.assertEquals;

// for [databind#1391]: should allow disabling of default typing
// via explicit {@link JsonTypeInfo}
class DefaultTypingOverride1391Test extends DatabindTestUtil {
    static class ListWrapper {
        /* 03-Oct-2016, tatu: This doesn't work because it applies to contents
         *   (elements), NOT the container. But there is no current mechanism
         *   to change that; would need to add a new feature or properties
         */
        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        public Collection<String> stuff = Collections.emptyList();
    }

    @Test
    void collectionWithOverride() throws Exception {
        final ObjectMapper mapper = JsonMapper.builder()
                .activateDefaultTypingAsProperty(NoCheckSubTypeValidator.instance,
                        ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE,
                        "$type")
                .build();
        String json = mapper.writeValueAsString(new ListWrapper());
        assertEquals(a2q("{'stuff':[]}"), json);
    }
}
