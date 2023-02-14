package com.fasterxml.jackson.databind.introspect;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.util.BeanUtil;

// [databind#2527] Support Kotlin-style "is" properties
public class IsGetterRenaming2527Test extends BaseMapTest
{
    static class POJO2527 {
        boolean isEnabled;

        protected POJO2527() { }
        public POJO2527(boolean b) {
            isEnabled = b;
        }

        public boolean getEnabled() { return isEnabled; }
        public void setEnabled(boolean b) { isEnabled = b; }
    }

    static class POJO2527PublicField {
        public boolean isEnabled;

        protected POJO2527PublicField() { }
        public POJO2527PublicField(boolean b) {
            isEnabled = b;
        }

        public boolean getEnabled() { return isEnabled; }
        public void setEnabled(boolean b) { isEnabled = b; }
    }

    static class POJO2527Creator {
        final boolean isEnabled;

        public POJO2527Creator(@JsonProperty("enabled") boolean b) {
            isEnabled = b;
        }

        public boolean getEnabled() { return isEnabled; }
    }

    @SuppressWarnings("serial")
    static class MyIntrospector extends JacksonAnnotationIntrospector
    {
        @Override
        public PropertyName findRenameByField(MapperConfig<?> config,
                AnnotatedField f, PropertyName implName)
        {
            final String origSimple = implName.getSimpleName();
            if (origSimple.startsWith("is")) {
                String mangledName = BeanUtil.stdManglePropertyName(origSimple, 2);
                // Needs to be valid ("is" -> null), and different from original
                if ((mangledName != null) && !mangledName.equals(origSimple)) {
                    return PropertyName.construct(mangledName);
                }
            }
            return null;
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .annotationIntrospector(new MyIntrospector())
            .build();

    public void testIsPropertiesStdKotlin() throws Exception
    {
        POJO2527 input = new POJO2527(true);
        final String json = MAPPER.writeValueAsString(input);

        Map<?, ?> props = MAPPER.readValue(json, Map.class);
        assertEquals(Collections.singletonMap("isEnabled", Boolean.TRUE),
                props);

        POJO2527 output = MAPPER.readValue(json, POJO2527.class);
        assertEquals(input.isEnabled, output.isEnabled);
    }

    public void testIsPropertiesWithPublicField() throws Exception
    {
        POJO2527PublicField input = new POJO2527PublicField(true);
        final String json = MAPPER.writeValueAsString(input);

        Map<?, ?> props = MAPPER.readValue(json, Map.class);
        assertEquals(Collections.singletonMap("isEnabled", Boolean.TRUE),
                props);

        POJO2527PublicField output = MAPPER.readValue(json, POJO2527PublicField.class);
        assertEquals(input.isEnabled, output.isEnabled);
    }

    public void testIsPropertiesViaCreator() throws Exception
    {
        POJO2527Creator input = new POJO2527Creator(true);
        final String json = MAPPER.writeValueAsString(input);

        Map<?, ?> props = MAPPER.readValue(json, Map.class);
        assertEquals(Collections.singletonMap("isEnabled", Boolean.TRUE),
                props);

        POJO2527Creator output = MAPPER.readValue(json, POJO2527Creator.class);
        assertEquals(input.isEnabled, output.isEnabled);
    }
}
