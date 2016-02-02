/******************************************************************************
 * * This data and information is proprietary to, and a valuable trade secret
 * * of, Basis Technology Corp.  It is given in confidence by Basis Technology
 * * and may only be used as permitted under the license agreement under which
 * * it has been distributed, and in no other way.
 * *
 * * Copyright (c) 2015 Basis Technology Corporation All rights reserved.
 * *
 * * The technical data and information provided herein are provided with
 * * `limited rights', and the computer software provided herein is provided
 * * with `restricted rights' as those terms are defined in DAR and ASPR
 * * 7-104.9(a).
 ******************************************************************************/

package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

@SuppressWarnings("serial")
public class ExceptionFromCustomEnumKeyDeserializerTest
    extends BaseMapTest
{
    public enum AnEnum {
        ZERO,
        ONE
    }

    public static class AnEnumDeserializer extends FromStringDeserializer<AnEnum> {

        public AnEnumDeserializer() {
            super(AnEnum.class);
        }

        //CHECKSTYLE:OFF
        @Override
        protected AnEnum _deserialize(String value, DeserializationContext ctxt) throws IOException {
            try {
                return AnEnum.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw ctxt.weirdKeyException(AnEnum.class, value, "Undefined AnEnum code");
            }
        }
    }

    public static class AnEnumKeyDeserializer extends KeyDeserializer {

        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
            try {
                return AnEnum.valueOf(key);
            } catch (IllegalArgumentException e) {
                throw ctxt.weirdKeyException(AnEnum.class, key, "Undefined AnEnum code");
            }
        }
    }


    @JsonDeserialize(using = AnEnumDeserializer.class, keyUsing = AnEnumKeyDeserializer.class)
    public enum LanguageCodeMixin {
    }

    public static class EnumModule extends SimpleModule {
        @Override
        public void setupModule(SetupContext context) {
            context.setMixInAnnotations(AnEnum.class, LanguageCodeMixin.class);
        }

        public static ObjectMapper setupObjectMapper(ObjectMapper mapper) {
            final EnumModule module = new EnumModule();
            mapper.registerModule(module);
            return mapper;
        }
    }

    @Test
    public void testLostMessage() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new EnumModule());
        try {
            objectMapper.readValue("{\"TWO\": \"dumpling\"}", new TypeReference<Map<AnEnum, String>>() {});
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Undefined AnEnum"));
            return;
        }
        fail("No exception");
    }
}
