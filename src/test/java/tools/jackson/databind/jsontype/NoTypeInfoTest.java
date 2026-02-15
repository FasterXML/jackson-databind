package tools.jackson.databind.jsontype;

import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

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

    // [databind#1391]
    static class ListWrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        public Collection<String> stuff = Collections.emptyList();
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    @Test
    public void testWithIdNone() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
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

    // [databind#1391]: should allow disabling of default typing
    // via explicit {@link JsonTypeInfo}
    @Test
    public void testCollectionWithOverride() throws Exception
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
