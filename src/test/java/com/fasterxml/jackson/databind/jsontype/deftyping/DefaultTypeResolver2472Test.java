package com.fasterxml.jackson.databind.jsontype.deftyping;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTypeResolverBuilder;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// [databind#2472]
public class DefaultTypeResolver2472Test extends DatabindTestUtil
{
    @Test
    public void testLegacyCtor2472() throws Exception
    {
        @SuppressWarnings({ "deprecation", "serial" })
        TypeResolverBuilder<?> legacyTyper = new DefaultTypeResolverBuilder(DefaultTyping.NON_FINAL) { };
        legacyTyper.init(JsonTypeInfo.Id.CLASS, null);
        legacyTyper.inclusion(JsonTypeInfo.As.PROPERTY);

        final ObjectMapper mapper = JsonMapper.builder()
                .setDefaultTyping(legacyTyper)
                .build();

        String json = mapper.writeValueAsString(Arrays.asList("foo"));
        assertNotNull(json);

        Object ob = mapper.readValue(json, Object.class);
        assertNotNull(ob);
    }
}
