package com.fasterxml.jackson.databind.jsontype;

import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class TestCustomTypeIdResolver extends BaseMapTest
{
    @JsonTypeInfo(use=Id.CUSTOM, include=As.WRAPPER_OBJECT)
    @JsonTypeIdResolver(CustomResolver.class)
    static abstract class CustomBean { }

    static class CustomBeanImpl extends CustomBean {
        public int x;

        public CustomBeanImpl() { }
        public CustomBeanImpl(int x) { this.x = x; }
    }

    static class ExtBeanWrapper {
        @JsonTypeInfo(use=Id.CUSTOM, include=As.EXTERNAL_PROPERTY, property="type")
        @JsonTypeIdResolver(ExtResolver.class)
        public ExtBean value;
    }

    static class CustomResolver extends TestCustomResolverBase {
        // yes, static: just for test purposes, not real use
        static List<JavaType> initTypes;

        public CustomResolver() {
            super(CustomBean.class, CustomBeanImpl.class);
        }

        @Override
        public void init(JavaType baseType) {
            if (initTypes != null) {
                initTypes.add(baseType);
            }
        }
    }

    static abstract class ExtBean { }

    static class ExtBeanImpl extends ExtBean {
        public int y;

        public ExtBeanImpl() { }
        public ExtBeanImpl(int y) { this.y = y; }
    }

    static class ExtResolver extends TestCustomResolverBase {
        public ExtResolver() {
            super(ExtBean.class, ExtBeanImpl.class);
        }
    }

    static class TestCustomResolverBase extends TypeIdResolverBase
    {
        protected final Class<?> superType;
        protected final Class<?> subType;

        public TestCustomResolverBase(Class<?> baseType, Class<?> implType) {
            superType = baseType;
            subType = implType;
        }

        @Override public Id getMechanism() { return Id.CUSTOM; }

        @Override public String idFromValue(Object value) {
            if (superType.isAssignableFrom(value.getClass())) {
                return "*";
            }
            return "unknown";
        }

        @Override
        public String idFromValueAndType(Object value, Class<?> type) {
            return idFromValue(value);
        }

        @Override
        public void init(JavaType baseType) { }

        @Override
        public JavaType typeFromId(DatabindContext context, String id)
        {
            if ("*".equals(id)) {
                return TypeFactory.defaultInstance().constructType(subType);
            }
            return null;
        }

        @Override
        public String idFromBaseType() {
            return "xxx";
        }
    }

    // for [databind#1270]
    static class Top1270 {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.PROPERTY,
                property = "type")
        @JsonTypeIdResolver(Resolver1270.class)
        public Base1270<?> b;
    }

    static class Base1270<O extends Poly1Base> {
        public O options;
        public String val;
    }

    static abstract class Poly1Base { }

    static class Poly1 extends Poly1Base {
        public String val;
    }

    static class Resolver1270 implements TypeIdResolver {
        public Resolver1270() { }

        @Override
        public void init(JavaType baseType) { }

        @Override
        public String idFromValue(Object value) {
            if (value.getClass() == Base1270.class) {
                return "poly1";
            }
            return null;
        }

        @Override
        public String idFromValueAndType(Object value, Class<?> suggestedType) {
            return idFromValue(value);
        }

        @Override
        public String idFromBaseType() {
            return null;
        }

        @Override
        public JavaType typeFromId(DatabindContext context, String id) {
            if ("poly1".equals(id)) {
                return context.getTypeFactory()
                        .constructType(new TypeReference<Base1270<Poly1>>() { });
            }
            return null;
        }

        @Override
        public String getDescForKnownTypeIds() {
            return null;
        }

        @Override
        public JsonTypeInfo.Id getMechanism() {
            return JsonTypeInfo.Id.CUSTOM;
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();

    public void testCustomTypeIdResolver() throws Exception
    {
        List<JavaType> types = new ArrayList<JavaType>();
        CustomResolver.initTypes = types;
        String json = MAPPER.writeValueAsString(new CustomBean[] { new CustomBeanImpl(28) });
        assertEquals("[{\"*\":{\"x\":28}}]", json);
        assertEquals(1, types.size());
        assertEquals(CustomBean.class, types.get(0).getRawClass());

        types = new ArrayList<JavaType>();
        CustomResolver.initTypes = types;
        CustomBean[] result = MAPPER.readValue(json, CustomBean[].class);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(28, ((CustomBeanImpl) result[0]).x);
        assertEquals(1, types.size());
        assertEquals(CustomBean.class, types.get(0).getRawClass());
    }

    public void testCustomWithExternal() throws Exception
    {
        ExtBeanWrapper w = new ExtBeanWrapper();
        w.value = new ExtBeanImpl(12);

        String json = MAPPER.writeValueAsString(w);

        ExtBeanWrapper out = MAPPER.readValue(json, ExtBeanWrapper.class);
        assertNotNull(out);

        assertEquals(12, ((ExtBeanImpl) out.value).y);
    }

    // for [databind#1270]
    public void testPolymorphicTypeViaCustom() throws Exception {
        Base1270<Poly1> req = new Base1270<Poly1>();
        Poly1 o = new Poly1();
        o.val = "optionValue";
        req.options = o;
        req.val = "some value";
        Top1270 top = new Top1270();
        top.b = req;
        String json = MAPPER.writeValueAsString(top);
        JsonNode tree = MAPPER.readTree(json);
        assertNotNull(tree.get("b"));
        assertNotNull(tree.get("b").get("options"));
        assertNotNull(tree.get("b").get("options").get("val"));

        // Can we reverse the process? I have some doubts
        Top1270 itemRead = MAPPER.readValue(json, Top1270.class);
        assertNotNull(itemRead);
        assertNotNull(itemRead.b);
    }
}
