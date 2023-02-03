package tools.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

public class NoTypeInfoTest extends BaseMapTest
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
}
