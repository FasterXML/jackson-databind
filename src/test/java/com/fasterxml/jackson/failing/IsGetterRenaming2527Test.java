package com.fasterxml.jackson.failing;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.util.BeanUtil;

// [databind#2527] Support Kotlin-style "is" properties
public class IsGetterRenaming2527Test extends BaseMapTest
{
    static class POJO2527 {
        private boolean isEnabled;

        protected POJO2527() { }
        public POJO2527(boolean b) {
            isEnabled = b;
        }

        public boolean getEnabled() { return isEnabled; }
        public void setEnabled(boolean b) { isEnabled = b; }
    }

    static class POJO2527b {
        public boolean isEnabled;

        protected POJO2527b() { }
        public POJO2527b(boolean b) {
            isEnabled = b;
        }

        public boolean getEnabled() { return isEnabled; }
        public void setEnabled(boolean b) { isEnabled = b; }
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
    
    private final ObjectMapper MAPPER = newJsonMapper();

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

    public void testIsPropertiesAlt() throws Exception
    {
        POJO2527b input = new POJO2527b(true);
        final String json = MAPPER.writeValueAsString(input);

        Map<?, ?> props = MAPPER.readValue(json, Map.class);
        assertEquals(Collections.singletonMap("isEnabled", Boolean.TRUE),
                props);
        
        POJO2527b output = MAPPER.readValue(json, POJO2527b.class);
        assertEquals(input.isEnabled, output.isEnabled);
    }
}
