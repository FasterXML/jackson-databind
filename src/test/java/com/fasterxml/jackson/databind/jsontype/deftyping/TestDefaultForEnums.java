package com.fasterxml.jackson.databind.jsontype.deftyping;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

public class TestDefaultForEnums
    extends BaseMapTest
{
    public enum TestEnum {
        A, B;
    }

    static final class EnumHolder
    {
        public Object value; // "untyped"

        public EnumHolder() { }
        public EnumHolder(TestEnum e) { value = e; }
    }

    protected static class TimeUnitBean {
        public TimeUnit timeUnit;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testSimpleEnumBean() throws Exception
    {
        TimeUnitBean bean = new TimeUnitBean();
        bean.timeUnit = TimeUnit.SECONDS;

        // First, without type info
        ObjectMapper m = new ObjectMapper();
        String json = m.writeValueAsString(bean);
        TimeUnitBean result = m.readValue(json, TimeUnitBean.class);
        assertEquals(TimeUnit.SECONDS, result.timeUnit);

        // then with type info
        m = JsonMapper.builder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance)
                .build();
        json = m.writeValueAsString(bean);
        result = m.readValue(json, TimeUnitBean.class);

        assertEquals(TimeUnit.SECONDS, result.timeUnit);
    }

    public void testSimpleEnumsInObjectArray() throws Exception
    {
        ObjectMapper m = JsonMapper.builder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance)
                .build();
        // Typing is needed for enums
        String json = m.writeValueAsString(new Object[] { TestEnum.A });
        assertEquals("[[\"com.fasterxml.jackson.databind.jsontype.deftyping.TestDefaultForEnums$TestEnum\",\"A\"]]", json);

        // and let's verify we get it back ok as well:
        Object[] value = m.readValue(json, Object[].class);
        assertEquals(1, value.length);
        assertSame(TestEnum.A, value[0]);
    }

    public void testSimpleEnumsAsField() throws Exception
    {
        ObjectMapper m = JsonMapper.builder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance)
                .build();
        String json = m.writeValueAsString(new EnumHolder(TestEnum.B));
        assertEquals("{\"value\":[\"com.fasterxml.jackson.databind.jsontype.deftyping.TestDefaultForEnums$TestEnum\",\"B\"]}", json);
        EnumHolder holder = m.readValue(json, EnumHolder.class);
        assertSame(TestEnum.B, holder.value);
    }
}
