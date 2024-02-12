package com.fasterxml.jackson.failing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class DeserializerUpdateProperties4358Test extends BaseMapTest {

    static class MutableBean {

        @JsonDeserialize(using = OrderingDeserializer.class)
        public String a;
        @JsonDeserialize(using = OrderingDeserializer.class)
        public String b;

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
        }

        public String getB() {
            return b;
        }

        public void setB(String b) {
            this.b = b;
        }
    }

    static class ImmutableBean {

        @JsonDeserialize(using = OrderingDeserializer.class)
        public final String a;
        @JsonDeserialize(using = OrderingDeserializer.class)
        public final String b;

        @JsonCreator
        public ImmutableBean(@JsonProperty("a") String a, @JsonProperty("b") String b) {
            this.a = a;
            this.b = b;
        }

        public String getA() {
            return a;
        }

        public String getB() {
            return b;
        }
    }

    static SimpleModule getSimpleModuleWithDeserializerModifier() {
        return new SimpleModule().setDeserializerModifier(new BeanDeserializerModifier() {
            private static final long serialVersionUID = 1L;

            @Override
            public List<BeanPropertyDefinition> updateProperties(DeserializationConfig config, BeanDescription beanDesc,
                    List<BeanPropertyDefinition> propDefs) {
                List<BeanPropertyDefinition> newPropDefs = new ArrayList<>(propDefs.size());
                for (BeanPropertyDefinition propDef : propDefs) {
                    if (propDef.getName().equals("b")) {
                        newPropDefs.add(0, propDef);
                    } else {
                        newPropDefs.add(propDef);
                    }
                }
                return newPropDefs;
            }
        });
    }

    static final List<String> actualOrder = new CopyOnWriteArrayList<>();
    
    static class OrderingDeserializer extends JsonDeserializer<String> {

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            String value = p.getValueAsString();
            actualOrder.add(value);
            return value;
        }
    }

    final ObjectMapper stdObjectMapper = jsonMapperBuilder().build();

    final ObjectMapper modifiedObjectMapper = jsonMapperBuilder().addModules(getSimpleModuleWithDeserializerModifier()).build();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        actualOrder.clear();
    }

    // Succeeds
    public void testMutableBeanStandard() throws Exception {
        MutableBean recreatedBean = stdObjectMapper.readValue("{\"a\": \"A\", \"b\": \"B\"}", MutableBean.class);

        assertEquals("A", recreatedBean.getA());
        assertEquals("B", recreatedBean.getB());
        assertEquals(Arrays.asList("A", "B"), actualOrder);
    }

    // Succeeds
    public void testImmutableBeanStandard() throws Exception {
        ImmutableBean recreatedBean = stdObjectMapper.readValue("{\"a\": \"A\", \"b\": \"B\"}", ImmutableBean.class);

        assertEquals("A", recreatedBean.getA());
        assertEquals("B", recreatedBean.getB());
        assertEquals(Arrays.asList("A", "B"), actualOrder);
    }

    // Fails - shall the order in the content really define the deserialization order? Probably not the most critical thing,
    // but may create subtitle issues with interdependencies.
    public void testMutableBeanReversedInputStandard() throws Exception {
        MutableBean recreatedBean = stdObjectMapper.readValue("{\"b\": \"B\", \"a\": \"A\"}", MutableBean.class);

        assertEquals("A", recreatedBean.getA());
        assertEquals("B", recreatedBean.getB());
        assertEquals(Arrays.asList("A", "B"), actualOrder);
    }

    // Fails - shall the order in the content really define the deserialization order? Probably not the most critical thing,
    // but may create subtitle issues with interdependencies.
    public void testImmutableBeanReversedInputStandard() throws Exception {
        ImmutableBean recreatedBean = stdObjectMapper.readValue("{\"b\": \"B\", \"a\": \"A\"}", ImmutableBean.class);

        assertEquals("A", recreatedBean.getA());
        assertEquals("B", recreatedBean.getB());
        assertEquals(Arrays.asList("A", "B"), actualOrder);
    }

    // Shall succeed - note that the setters are called in the same order as the deserializers.
    public void testMutableBeanUpdateOrderBuilder() throws Exception {
        MutableBean recreatedBean = modifiedObjectMapper.readValue("{\"a\": \"A\", \"b\": \"B\"}", MutableBean.class);

        assertEquals("A", recreatedBean.getA());
        assertEquals("B", recreatedBean.getB());
        assertEquals(Arrays.asList("B", "A"), actualOrder);
    }

    // Shall succeed
    public void testImmutableBeanUpdateOrderBuilder() throws Exception {
        ImmutableBean recreatedBean = modifiedObjectMapper.readValue("{\"a\": \"A\", \"b\": \"B\"}", ImmutableBean.class);

        assertEquals("A", recreatedBean.getA());
        assertEquals("B", recreatedBean.getB());
        assertEquals(Arrays.asList("B", "A"), actualOrder);
    }
}
