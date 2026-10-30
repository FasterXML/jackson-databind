package com.fasterxml.jackson.databind.deser.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.CreatorProperty;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

// [databind#6338]: readerForUpdating() must honor Nulls.SKIP for
// creator properties too
public class SkipNullsCreatorUpdate6338Test
    extends DatabindTestUtil
{
    static class FieldBean {
        @JsonSetter(nulls = Nulls.SKIP)
        public List<String> values;
        public String name;

        @JsonCreator
        public FieldBean(@JsonProperty("values") List<String> values) {
            this.values = values;
        }
    }

    static class SetterBean {
        protected List<String> values;
        public String name;

        @JsonCreator
        public SetterBean(@JsonProperty("values") List<String> values) {
            this.values = values;
        }

        @JsonSetter(nulls = Nulls.SKIP)
        public void setValues(List<String> values) {
            this.values = values;
        }
    }

    static class PlainBean {
        public List<String> values;
        public String name;

        @JsonCreator
        public PlainBean(@JsonProperty("values") List<String> values) {
            this.values = values;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    private final static String JSON = a2q("{'values':null,'name':'x'}");

    @Test
    public void testSkipNullsForCreatorField() throws Exception
    {
        FieldBean bean = new FieldBean(new ArrayList<>(Arrays.asList("a")));
        MAPPER.readerForUpdating(bean).readValue(JSON);
        assertEquals("x", bean.name);
        assertEquals(Arrays.asList("a"), bean.values);
    }

    @Test
    public void testSkipNullsForCreatorSetter() throws Exception
    {
        SetterBean bean = new SetterBean(new ArrayList<>(Arrays.asList("a")));
        MAPPER.readerForUpdating(bean).readValue(JSON);
        assertEquals("x", bean.name);
        assertEquals(Arrays.asList("a"), bean.values);
    }

    @Test
    public void testSkipNullsForCreatorGlobalDefault() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .defaultSetterInfo(JsonSetter.Value.forValueNulls(Nulls.SKIP))
                .build();
        PlainBean bean = new PlainBean(new ArrayList<>(Arrays.asList("a")));
        mapper.readerForUpdating(bean).readValue(JSON);
        assertEquals("x", bean.name);
        assertEquals(Arrays.asList("a"), bean.values);
    }

    // Without "skip nulls", null is still set as before
    @Test
    public void testNullSetForCreatorWithoutSkip() throws Exception
    {
        PlainBean bean = new PlainBean(new ArrayList<>(Arrays.asList("a")));
        MAPPER.readerForUpdating(bean).readValue(JSON);
        assertEquals("x", bean.name);
        assertNull(bean.values);
    }

    // Values coerced into null (here, empty String) must be skipped as well
    @Test
    public void testSkipNullsForCreatorCoercedNull() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .withCoercionConfigDefaults(cfg ->
                    cfg.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull))
                .build();
        FieldBean bean = new FieldBean(new ArrayList<>(Arrays.asList("a")));
        mapper.readerForUpdating(bean).readValue(a2q("{'values':'','name':'x'}"));
        assertEquals("x", bean.name);
        assertEquals(Arrays.asList("a"), bean.values);
    }

    // Other mutation methods of CreatorProperty must also skip nulls
    @Test
    public void testSkipNullsForCreatorSetMethods() throws Exception
    {
        try (JsonParser p = MAPPER.createParser("null")) {
            DeserializationContext ctxt = ((DefaultDeserializationContext) MAPPER.getDeserializationContext())
                    .createInstance(MAPPER.getDeserializationConfig(), p, null);
            BeanDeserializerBase deser = (BeanDeserializerBase) ctxt.findRootValueDeserializer(
                    ctxt.constructType(FieldBean.class));
            SettableBeanProperty prop = deser.findProperty("values");
            assertInstanceOf(CreatorProperty.class, prop);

            List<String> orig = new ArrayList<>(Arrays.asList("a"));
            FieldBean bean = new FieldBean(orig);

            assertEquals(JsonToken.VALUE_NULL, p.nextToken());
            assertSame(bean, prop.deserializeSetAndReturn(p, ctxt, bean));
            assertSame(orig, bean.values);

            prop.set(bean, null);
            assertSame(orig, bean.values);
            assertSame(bean, prop.setAndReturn(bean, null));
            assertSame(orig, bean.values);

            // but non-null values still set
            List<String> other = Arrays.asList("b");
            prop.set(bean, other);
            assertSame(other, bean.values);
        }
    }
}
