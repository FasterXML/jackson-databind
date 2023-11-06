package tools.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

// for [databind#1391]: should allow disabling of default typing
// via explicit {@link JsonTypeInfo}
public class DefaultTypingOverride1391Test extends BaseMapTest
{
    static class ListWrapper {
        /* 03-Oct-2016, tatu: This doesn't work because it applies to contents
         *   (elements), NOT the container. But there is no current mechanism
         *   to change that; would need to add a new feature or properties
         */
        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        public Collection<String> stuff = Collections.emptyList();
    }

    public void testCollectionWithOverride() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .activateDefaultTypingAsProperty(NoCheckSubTypeValidator.instance,
                    DefaultTyping.OBJECT_AND_NON_CONCRETE,
                    "$type")
            .build();
        String json = mapper.writeValueAsString(new ListWrapper());
        assertEquals(a2q("{'stuff':[]}"), json);
    }
}
