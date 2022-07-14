package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import tools.jackson.core.Version;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class TestSubtypesWithDefaultImpl extends BaseMapTest
{
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=As.PROPERTY,
            property="#type",
            defaultImpl=DefaultImpl.class)
    static abstract class SuperTypeWithDefault { }

    static class DefaultImpl extends SuperTypeWithDefault {
        public int a;
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=As.PROPERTY, property="#type")
    static abstract class SuperTypeWithoutDefault { }

    static class DefaultImpl505 extends SuperTypeWithoutDefault {
        public int a;
    }
    
    /*
    /**********************************************************************
    /* Unit tests
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testDefaultImpl() throws Exception
    {
        // first, test with no type information
        SuperTypeWithDefault bean = MAPPER.readValue("{\"a\":13}", SuperTypeWithDefault.class);
        assertEquals(DefaultImpl.class, bean.getClass());
        assertEquals(13, ((DefaultImpl) bean).a);

        // and then with unmapped info
        bean = MAPPER.readValue("{\"a\":14,\"#type\":\"foobar\"}", SuperTypeWithDefault.class);
        assertEquals(DefaultImpl.class, bean.getClass());
        assertEquals(14, ((DefaultImpl) bean).a);

        bean = MAPPER.readValue("{\"#type\":\"foobar\",\"a\":15}", SuperTypeWithDefault.class);
        assertEquals(DefaultImpl.class, bean.getClass());
        assertEquals(15, ((DefaultImpl) bean).a);

        bean = MAPPER.readValue("{\"#type\":\"foobar\"}", SuperTypeWithDefault.class);
        assertEquals(DefaultImpl.class, bean.getClass());
        assertEquals(0, ((DefaultImpl) bean).a);
    }

    // [JACKSON-505]: ok to also default to mapping there might be for base type
    public void testDefaultImplViaModule() throws Exception
    {
        final String JSON = "{\"a\":123}";
        
        // first: without registration etc, epic fail:
        try {
            MAPPER.readValue(JSON, SuperTypeWithoutDefault.class);
            fail("Expected an exception");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "missing type id property '#type'");
        }

        // but then succeed when we register default impl
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addAbstractTypeMapping(SuperTypeWithoutDefault.class, DefaultImpl505.class);
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();
        SuperTypeWithoutDefault bean = mapper.readValue(JSON, SuperTypeWithoutDefault.class);
        assertNotNull(bean);
        assertEquals(DefaultImpl505.class, bean.getClass());
        assertEquals(123, ((DefaultImpl505) bean).a);

        bean = mapper.readValue("{\"#type\":\"foobar\"}", SuperTypeWithoutDefault.class);
        assertEquals(DefaultImpl505.class, bean.getClass());
        assertEquals(0, ((DefaultImpl505) bean).a);
    }
}
