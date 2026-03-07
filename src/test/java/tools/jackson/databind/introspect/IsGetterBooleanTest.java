package tools.jackson.databind.introspect;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IsGetterBooleanTest extends DatabindTestUtil
{
    // [databind#3609]
    static class POJO3609 {
        int isEnabled;

        protected POJO3609() { }
        public POJO3609(int b) {
            isEnabled = b;
        }

        public int isEnabled() { return isEnabled; }
        public void setEnabled(int b) { isEnabled = b; }
    }

    // [databind#3836]
    static class POJO3836_AR {
        public AtomicReference<Boolean> isAtomic() {
            return new AtomicReference<>(true);
        }
    }

    // [databind#3836]
    static class POJO3836_AB {
        public AtomicBoolean isAtomic() {
            return new AtomicBoolean(true);
        }
    }

    static class POJO3836_OB {
        public Optional<Boolean> isAtomic() {
            return Optional.of(true);
        }
    }

    // [databind#2527]
    static class POJO2527 {
        boolean isEnabled;

        protected POJO2527() { }
        public POJO2527(boolean b) {
            isEnabled = b;
        }

        public boolean getEnabled() { return isEnabled; }
        public void setEnabled(boolean b) { isEnabled = b; }
    }

    // [databind#2527]
    static class POJO2527PublicField {
        public boolean isEnabled;

        protected POJO2527PublicField() { }
        public POJO2527PublicField(boolean b) {
            isEnabled = b;
        }

        public boolean getEnabled() { return isEnabled; }
        public void setEnabled(boolean b) { isEnabled = b; }
    }

    // [databind#2527]
    static class POJO2527Creator {
        boolean isEnabled;

        public POJO2527Creator(@JsonProperty("enabled") boolean b) {
            isEnabled = b;
        }

        public boolean getEnabled() { return isEnabled; }
    }

    @SuppressWarnings("serial")
    static class IsGetterRenamingIntrospector extends JacksonAnnotationIntrospector
    {
        @Override
        public PropertyName findRenameByField(MapperConfig<?> config,
                AnnotatedField f, PropertyName implName)
        {
            final String origSimple = implName.getSimpleName();
            if (origSimple.startsWith("is")) {
                String mangledName = stdManglePropertyName(origSimple, 2);
                if ((mangledName != null) && !mangledName.equals(origSimple)) {
                    return PropertyName.construct(mangledName);
                }
            }
            return null;
        }

        protected String stdManglePropertyName(final String basename, final int offset)
        {
            final int end = basename.length();
            char c0 = basename.charAt(offset);
            char c1 = Character.toLowerCase(c0);
            if (c0 == c1) {
                return basename.substring(offset);
            }
            if ((offset + 1) < end) {
                if (Character.isUpperCase(basename.charAt(offset+1))) {
                    return basename.substring(offset);
                }
            }
            StringBuilder sb = new StringBuilder(end - offset);
            sb.append(c1);
            sb.append(basename, offset+1, end);
            return sb.toString();
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    // [databind#3609]
    @Test
    public void testAllowIntIsGetter() throws Exception
    {
        ObjectMapper MAPPER = jsonMapperBuilder()
                .enable(MapperFeature.ALLOW_IS_GETTERS_FOR_NON_BOOLEAN)
                .build();

        POJO3609 input = new POJO3609(12);
        final String json = MAPPER.writeValueAsString(input);

        Map<?, ?> props = MAPPER.readValue(json, Map.class);
        assertEquals(Collections.singletonMap("enabled", 12),
                props);

        POJO3609 output = MAPPER.readValue(json, POJO3609.class);
        assertEquals(input.isEnabled, output.isEnabled);
    }

    // [databind#3609]
    @Test
    public void testDisallowIntIsGetter() throws Exception
    {
        ObjectMapper MAPPER = jsonMapperBuilder()
                .disable(MapperFeature.ALLOW_IS_GETTERS_FOR_NON_BOOLEAN)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .build();

        POJO3609 input = new POJO3609(12);
        final String json = MAPPER.writeValueAsString(input);

        assertEquals("{}", json);

    }

    // [databind#3836]
    @Test
    public void testBooleanReference() throws Exception
    {
        assertEquals(a2q("{'atomic':true}"),
                sharedMapper().writeValueAsString(new POJO3836_AR()));
    }

    // [databind#3836]
    @Test
    public void testAtomicBoolean() throws Exception
    {
        assertEquals(a2q("{'atomic':true}"),
                sharedMapper().writeValueAsString(new POJO3836_AB()));
    }

    // [databind#3836]
    @Test
    public void testOptionalBoolean() throws Exception
    {
        assertEquals(a2q("{'atomic':true}"),
                sharedMapper().writeValueAsString(new POJO3836_OB()));
    }

    /*
    /**********************************************************************
    /* Test methods, "is" property renaming [databind#2527]
    /**********************************************************************
     */

    // [databind#2527]
    @Test
    public void testIsPropertiesStdKotlin() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .annotationIntrospector(new IsGetterRenamingIntrospector())
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .build();

        POJO2527 input = new POJO2527(true);
        final String json = mapper.writeValueAsString(input);

        Map<?, ?> props = mapper.readValue(json, Map.class);
        assertEquals(Collections.singletonMap("isEnabled", Boolean.TRUE),
                props);

        POJO2527 output = mapper.readValue(json, POJO2527.class);
        assertEquals(input.isEnabled, output.isEnabled);
    }

    // [databind#2527]
    @Test
    public void testIsPropertiesWithPublicField() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .annotationIntrospector(new IsGetterRenamingIntrospector())
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .build();

        POJO2527PublicField input = new POJO2527PublicField(true);
        final String json = mapper.writeValueAsString(input);

        Map<?, ?> props = mapper.readValue(json, Map.class);
        assertEquals(Collections.singletonMap("isEnabled", Boolean.TRUE),
                props);

        POJO2527PublicField output = mapper.readValue(json, POJO2527PublicField.class);
        assertEquals(input.isEnabled, output.isEnabled);
    }

    // [databind#2527]
    @Test
    public void testIsPropertiesViaCreator() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .annotationIntrospector(new IsGetterRenamingIntrospector())
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .build();

        POJO2527Creator input = new POJO2527Creator(true);
        final String json = mapper.writeValueAsString(input);

        Map<?, ?> props = mapper.readValue(json, Map.class);
        assertEquals(Collections.singletonMap("isEnabled", Boolean.TRUE),
                props);

        POJO2527Creator output = mapper.readValue(json, POJO2527Creator.class);
        assertEquals(input.isEnabled, output.isEnabled);
    }
}
