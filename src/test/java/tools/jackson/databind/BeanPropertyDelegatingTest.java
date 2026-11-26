package tools.jackson.databind;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BeanProperty.Delegating} (added for [databind#5742])
 */
public class BeanPropertyDelegatingTest extends DatabindTestUtil
{
    // Guard against methods of BeanProperty (or super-interfaces) not being
    // delegated: compiler catches abstract ones, but not `default` methods
    @Test
    public void allMethodsDelegated() throws Exception
    {
        for (Method m : BeanProperty.class.getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            Method impl = BeanProperty.Delegating.class.getMethod(m.getName(), m.getParameterTypes());
            assertEquals(BeanProperty.Delegating.class, impl.getDeclaringClass(),
                    "Method not overridden by `BeanProperty.Delegating`: "+m);
        }
    }

    @Test
    public void callsForwardedToDelegate()
    {
        final JavaType type = defaultTypeFactory().constructType(String.class);
        final PropertyName name = PropertyName.construct("prop");
        final PropertyName wrapperName = PropertyName.construct("wrapper");
        BeanProperty delegate = new BeanProperty.Std(name, type, wrapperName,
                null, PropertyMetadata.STD_REQUIRED);
        BeanProperty prop = new BeanProperty.Delegating(delegate);

        assertEquals("prop", prop.getName());
        assertSame(name, prop.getFullName());
        assertTrue(prop.hasName(name));
        assertSame(type, prop.getType());
        assertSame(wrapperName, prop.getWrapperName());
        assertSame(PropertyMetadata.STD_REQUIRED, prop.getMetadata());
        assertTrue(prop.isRequired());
        assertFalse(prop.isVirtual());
        assertNull(prop.getMember());
        assertNull(prop.getAnnotation(Deprecated.class));
        assertNull(prop.getContextAnnotation(Deprecated.class));
        assertEquals(0, prop.findAliases(null).size());
    }

    // Overriding one method only affects that method
    @Test
    public void overrideOfTypeOnly()
    {
        final JavaType stringType = defaultTypeFactory().constructType(String.class);
        final JavaType intType = defaultTypeFactory().constructType(Integer.class);
        BeanProperty delegate = new BeanProperty.Std(PropertyName.construct("prop"),
                stringType, null, null, PropertyMetadata.STD_OPTIONAL);
        BeanProperty prop = new BeanProperty.Delegating(delegate) {
            @Override
            public JavaType getType() { return intType; }
        };
        assertSame(intType, prop.getType());
        assertEquals("prop", prop.getName());
        assertSame(PropertyMetadata.STD_OPTIONAL, prop.getMetadata());
    }
}
