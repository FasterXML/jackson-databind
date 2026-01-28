package tools.jackson.databind.jsontype;

import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.assertEquals;

// for [databind#1391]: should allow disabling of default typing
// via explicit {@link JsonTypeInfo}
class DefaultTypingOverride1391Test extends DatabindTestUtil {
    static class ListWrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        public Collection<String> stuff = Collections.emptyList();
    }

    @Test
    void collectionWithOverride() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .activateDefaultTypingAsProperty(NoCheckSubTypeValidator.instance,
                    DefaultTyping.OBJECT_AND_NON_CONCRETE,
                    "$type")
            .build();
        String json = mapper.writeValueAsString(new ListWrapper());
        assertEquals(a2q("{'stuff':[]}"), json);

        // And verify deserialization works too
        ListWrapper result = mapper.readValue(json, ListWrapper.class);
        assertEquals(0, result.stuff.size());
    }
}
