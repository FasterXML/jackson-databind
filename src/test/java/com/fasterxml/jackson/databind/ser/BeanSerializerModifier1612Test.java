package com.fasterxml.jackson.databind.ser;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class BeanSerializerModifier1612Test extends BaseMapTest
{
    @JsonPropertyOrder({ "a", "b", "c" })
    static class Bean1612 {
        public Integer a;
        public Integer b;
        public Double c;

        public Bean1612(Integer a, Integer b, Double c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    static class Modifier1612 extends BeanSerializerModifier {
        @Override
        public BeanSerializerBuilder updateBuilder(SerializationConfig config, BeanDescription beanDesc,
                BeanSerializerBuilder builder) {
            List<BeanPropertyWriter> filtered = new ArrayList<BeanPropertyWriter>(2);
            List<BeanPropertyWriter> properties = builder.getProperties();
            //Make the filtered properties list bigger
            builder.setFilteredProperties(new BeanPropertyWriter[] {properties.get(0), properties.get(1), properties.get(2)});

            //The props will be shorter
            filtered.add(properties.get(1));
            filtered.add(properties.get(2));
            builder.setProperties(filtered);
            return builder;
        }
    }

    /*
    /**********************************************************
    /* Construction and setter methods
    /**********************************************************
     */

    public void testIssue1612() throws Exception
    {
        SimpleModule mod = new SimpleModule();
        mod.setSerializerModifier(new Modifier1612());
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(mod);
        try {
            objectMapper.writeValueAsString(new Bean1612(0, 1, 2d));
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Failed to construct BeanSerializer");
            verifyException(e, Bean1612.class.getName());
        }
    }
}
